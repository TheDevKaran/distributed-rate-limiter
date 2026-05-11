package com.example.redis.fixed;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

import java.util.Arrays;
import java.util.Collections;

public class FixedWindowLimiter {

    private final JedisPool pool;
    private final int maxReq;
    private final int windowTime;
    private final String scriptSha;

    private static final String LUA_SCRIPT = """
        local current = redis.call('GET', KEYS[1])
        if not current then
            redis.call('SET', KEYS[1], 1, 'EX', ARGV[2], 'NX')
            return 1
        end
        if tonumber(current) < tonumber(ARGV[1]) then
            return redis.call('INCR', KEYS[1])
        else
            return -1
        end
        """;

    // default constructor — reads from env or falls back to localhost
    public FixedWindowLimiter(int maxReq, int windowTime) {
        this(
            maxReq,
            windowTime,
            System.getenv().getOrDefault("REDIS_HOST", "localhost"),
            Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"))
        );
    }

    // injectable constructor — for testing with different Redis instances
    public FixedWindowLimiter(int maxReq, int windowTime, String host, int port) {
        this.maxReq = maxReq;
        this.windowTime = windowTime;

        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(50);         // max connections in pool
        config.setMaxIdle(10);          // max idle connections
        config.setMinIdle(2);           // min idle connections kept warm
        config.setTestOnBorrow(true);   // validate connection before use

        this.pool = new JedisPool(config, host, port);

        try (Jedis jedis = pool.getResource()) {
            this.scriptSha = jedis.scriptLoad(LUA_SCRIPT);
        } catch (JedisException e) {
            throw new RuntimeException("Failed to connect to Redis at " + host + ":" + port, e);
        }
    }

    public boolean allowedReq(String userId) {
        if (maxReq <= 0)
            return false;

        String key = "rate_limit:fixed:" + userId;

        int attempts = 0;

        while (attempts < 3) {
            try (Jedis jedis = pool.getResource()) {
                Object result = jedis.evalsha(
                        scriptSha,
                        Collections.singletonList(key),
                        Arrays.asList(
                                String.valueOf(maxReq),
                                String.valueOf(windowTime)
                        )
                );
                return (Long) result != -1;

            } catch (JedisException e) {
                attempts++;
                if (attempts == 3) {
                    // Redis is down — fail open (allow) or fail closed (block)
                    // fail open here: don't punish users for our infrastructure issues
                    System.err.println("Redis unavailable after 3 attempts, failing open: " + e.getMessage());
                    return true;
                }
            }
        }

        return true; // unreachable but compiler needs it
    }

    public void clearUser(String userId) {
        try (Jedis jedis = pool.getResource()) {
            jedis.del("rate_limit:fixed:" + userId);
        } catch (JedisException e) {
            System.err.println("Failed to clear user: " + e.getMessage());
        }
    }

    public void close() {
        pool.close();
    }
}