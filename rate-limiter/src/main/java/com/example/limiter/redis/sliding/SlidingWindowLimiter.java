package com.example.limiter.redis.sliding;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

import java.util.Arrays;
import java.util.Collections;

public class SlidingWindowLimiter {

    private final JedisPool pool;
    private final int maxReq;
    private final int windowTime;
    private final String scriptSha;

    private static final String LUA_SCRIPT = """
        local key = KEYS[1]
        local currentTime = tonumber(ARGV[1])
        local windowTime  = tonumber(ARGV[2])
        local maxReq      = tonumber(ARGV[3])

        local windowStart = currentTime - windowTime

        -- remove expired requests
        redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)

        -- count active requests in window
        local currentCount = redis.call('ZCARD', key)

        -- reject if limit exceeded
        if currentCount >= maxReq then
            return -1
        end

        -- use unique member to avoid duplicate collapse
        -- score = timestamp (for range queries)
        -- member = timestamp + random suffix (for uniqueness)
        local uniqueMember = tostring(currentTime) .. ':' .. tostring(redis.call('INCR', key .. ':seq'))

        redis.call('ZADD', key, currentTime, uniqueMember)
        redis.call('EXPIRE', key, windowTime + 1)
        redis.call('EXPIRE', key .. ':seq', windowTime + 1)  


        return currentCount + 1
        """;

    public SlidingWindowLimiter(int maxReq, int windowTime) {
        this(
            maxReq,
            windowTime,
            System.getenv().getOrDefault("REDIS_HOST", "localhost"),
            Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"))
        );
    }

    public SlidingWindowLimiter(int maxReq, int windowTime, String host, int port) {
        this.maxReq = maxReq;
        this.windowTime = windowTime;

        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(50);
        config.setMaxIdle(10);
        config.setMinIdle(2);
        config.setTestOnBorrow(true);

        this.pool = new JedisPool(config, host, port);

        try (Jedis jedis = pool.getResource()) {
            this.scriptSha = jedis.scriptLoad(LUA_SCRIPT);
        } catch (JedisException e) {
            throw new RuntimeException(
                "Failed to connect to Redis at " + host + ":" + port, e
            );
        }
    }

    public boolean allowedReq(String userId) {
        if (maxReq <= 0)
            return false;

        String key = "rate_limit:sliding:" + userId;
        long currentTime = System.currentTimeMillis() / 1000;

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
        try (Jedis jedis = pool.getResource()) {
            jedis.del(
                "rate_limit:sliding:" + userId,
                "rate_limit:sliding:" + userId + ":seq"
            );
        } catch (JedisException e) {
            System.err.println("Failed to clear user: " + e.getMessage());
        }
    }

    public void close() {
        pool.close();
    }
}