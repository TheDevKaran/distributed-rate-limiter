package com.example.limiter.redis.sliding;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

import java.util.Arrays;
import java.util.Collections;

public class SlidingWindowLimiter {

    private final JedisPool pool;
    private final int maxReq;
    private final int windowTime;
    private final String scriptSha;
    private final String policyName;

    private static final String LUA_SCRIPT = """
        local key = KEYS[1]
        local currentTime = tonumber(ARGV[1])
        local windowTime  = tonumber(ARGV[2])
        local maxReq      = tonumber(ARGV[3])

        local windowStart = currentTime - windowTime

        redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)

        local currentCount = redis.call('ZCARD', key)

        if currentCount >= maxReq then
            return -1
        end

        local uniqueMember =
            tostring(currentTime)
            .. ':'
            .. tostring(redis.call('INCR', key .. ':seq'))

        redis.call('ZADD', key, currentTime, uniqueMember)

        redis.call('EXPIRE', key, windowTime + 1)
        redis.call('EXPIRE', key .. ':seq', windowTime + 1)

        return currentCount + 1
        """;

    public SlidingWindowLimiter(
            JedisPool pool,
            int maxReq,
            int windowTime,
            String policyName
    ) {

        this.pool = pool;
        this.maxReq = maxReq;
        this.windowTime = windowTime;
        this.policyName = policyName;

        try (Jedis jedis = pool.getResource()) {
            this.scriptSha = jedis.scriptLoad(LUA_SCRIPT);
        }
    }

    public boolean allowedReq(String userId) {

        if (maxReq <= 0)
            return false;

        String key =
                "rate_limit:"
                + policyName
                + ":"
                + userId;

        long currentTime =
                System.currentTimeMillis() / 1000;

        int attempts = 0;

        while (attempts < 3) {

            try (Jedis jedis = pool.getResource()) {

                Object result = jedis.evalsha(
                        scriptSha,
                        Collections.singletonList(key),
                        Arrays.asList(
                                String.valueOf(currentTime),
                                String.valueOf(windowTime),
                                String.valueOf(maxReq)
                        )
                );

                return (Long) result != -1;

            } catch (JedisException e) {

                attempts++;

                if (attempts == 3) {

                    System.err.println(
                            "Redis unavailable after 3 attempts, failing open: "
                            + e.getMessage()
                    );

                    return true;
                }
            }
        }

        return true;
    }

    public void clearUser(String userId) {

        String key =
                "rate_limit:"
                + policyName
                + ":"
                + userId;

        try (Jedis jedis = pool.getResource()) {

            jedis.del(
                    key,
                    key + ":seq"
            );

        } catch (JedisException e) {

            System.err.println(
                    "Failed to clear user: "
                    + e.getMessage()
            );
        }
    }

    public void close() {
        pool.close();
    }
}