package com.example.limiter.redis.sliding;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.example.DTO.RateLimitResult;
import com.example.limiter.RateLimiter;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

public class SlidingWindowLimiter implements RateLimiter{

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

            local oldestRequest =
                redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')

            local oldestTimestamp =
                tonumber(oldestRequest[2])

            local retryAfter =
                oldestTimestamp + windowTime - currentTime

            return {0, 0, retryAfter}
        end

        local uniqueMember =
            tostring(currentTime)
            .. ':'
            .. tostring(redis.call('INCR', key .. ':seq'))

        redis.call('ZADD', key, currentTime, uniqueMember)

        redis.call('EXPIRE', key, windowTime + 1)
        redis.call('EXPIRE', key .. ':seq', windowTime + 1)

        local remaining = maxReq - (currentCount + 1)
        return {1, remaining, 0}
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

    public RateLimitResult allowRequest(String userId) {

        if (maxReq <= 0)
            return new RateLimitResult(
                    false,
                    0,
                    windowTime
            );

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

                List<Long> response =
                        (List<Long>) result;

                boolean allowed =
                        response.get(0) == 1;

                long remaining =
                        response.get(1);

                long retryAfter =
                        response.get(2);

                return new RateLimitResult(
                        allowed,
                        (int) remaining,
                        retryAfter
                );

            } catch (JedisException e) {

                attempts++;

                if (attempts == 3) {

                    System.err.println(
                            "Redis unavailable after 3 attempts, failing open: "
                            + e.getMessage()
                    );

                    return new RateLimitResult(
                            true,
                            maxReq,
                            0
                    );
                }
            }
        }

        return new RateLimitResult(
                true,
                maxReq,
                0
        );
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