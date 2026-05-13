package com.example.limiter.redis.fixed;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
// import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

import java.util.Arrays;
import java.util.Collections;

import com.example.DTO.RateLimitResult;

public class FixedWindowLimiter {

    private final JedisPool pool;
    private final int maxReq;
    private final int windowTime;
    private final String scriptSha;
    private final String policyName;

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
public FixedWindowLimiter(
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

    public RateLimitResult allowedReq(String userId) {
        if (maxReq <= 0)
            return new RateLimitResult(false, 0, 0);

        String key = "rate_limit:" + policyName + ":" + userId;

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
                Long currentCount = (Long) result;

                long ttl = jedis.ttl(key);

                if (currentCount == -1) {
                    return new RateLimitResult(
                            false,
                            0,
                            ttl
                    );
                }

                long remaining =
                        Math.max(0, maxReq - currentCount);

                return new RateLimitResult(
                        true,
                        remaining,
                        ttl
                );

            } catch (JedisException e) {
                attempts++;
                if (attempts == 3) {
                    // Redis is down — fail open (allow) or fail closed (block)
                    // fail open here: don't punish users for our infrastructure issues
                    System.err.println("Redis unavailable after 3 attempts, failing open: " + e.getMessage());
                    return new RateLimitResult(true, -1, -1);
                }
            }
        }

        return new RateLimitResult(true, -1, -1); // unreachable but compiler needs it
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