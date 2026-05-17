package com.example.limiter.redis.token;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.example.DTO.RateLimitResult;
import com.example.limiter.RateLimiter;

public class TokenBucketRateLimiter implements RateLimiter {

    private final JedisPool pool;   
    private final long capacity;
    private final double refillRate;
    private final String scriptSha;
    private final String policyName;

    // ttl = how long before an idle bucket expires from Redis
    // default: enough time for bucket to fully refill from empty
    private final long ttlSeconds;

    private static final String LUA_SCRIPT = """
        local key          = KEYS[1]
        local capacity     = tonumber(ARGV[1])
        local refillRate   = tonumber(ARGV[2])
        local now          = tonumber(ARGV[3])

        -- fetch existing state
        local data = redis.call('HMGET', key, 'tokens', 'lastRefillTime')
        local tokens        = tonumber(data[1])
        local lastRefillTime = tonumber(data[2])

        -- first request ever for this user
        if not tokens then
            tokens = capacity - 1
            redis.call('HMSET', key,
                'tokens', tokens,
                'lastRefillTime', now
            )
            redis.call('EXPIRE', key, ARGV[4])
            return {1, math.floor(tokens), 0}
        end

        -- refill based on elapsed time
        local timePassed  = (now - lastRefillTime) / 1000.0
        local tokensToAdd = timePassed * refillRate

        -- round to 4 decimal places to avoid float drift
        tokens = math.min(capacity, tokens + tokensToAdd)
        tokens = math.floor(tokens * 10000 + 0.5) / 10000

        -- consume token
        if tokens >= 1 then
            tokens = tokens - 1
            tokens = math.floor(tokens * 10000 + 0.5) / 10000

            redis.call('HMSET', key,    
                'tokens', tokens,
                'lastRefillTime', now
            )
            redis.call('EXPIRE', key, ARGV[4])
            return {1, math.floor(tokens), 0}
        end

        -- not enough tokens — still update lastRefillTime
        -- so partial tokens keep accumulating correctly
        redis.call('HMSET', key,
            'tokens', tokens,
            'lastRefillTime', now
        )
        redis.call('EXPIRE', key, ARGV[4])
        local retryAfter = math.ceil((1 - tokens) / refillRate)

        return {0, math.floor(tokens), retryAfter}        
""";

   public TokenBucketRateLimiter(
        JedisPool pool,
        long capacity,
        double refillRate,
        String policyName
    ) {

        if (capacity < 0)
            throw new IllegalArgumentException("Capacity must be >= 0");

        if (refillRate < 0)
            throw new IllegalArgumentException("Refill rate must be >= 0");

        this.pool = pool;
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.policyName = policyName;

        this.ttlSeconds = refillRate > 0
                ? (long) Math.ceil(capacity / refillRate) + 60
                : 3600;

        try (Jedis jedis = pool.getResource()) {
            this.scriptSha = jedis.scriptLoad(LUA_SCRIPT);
        }
    }

    public RateLimitResult allowRequest(String userId) {
        if (capacity <= 0)
            return new RateLimitResult(
                    false,
                    0,
                    1
            );
        String key = "rate_limit:" + policyName + ":" + userId;        
        long now   = System.currentTimeMillis();

        int attempts = 0;

        while (attempts < 3) {
            try (Jedis jedis = pool.getResource()) {
                Object result = jedis.evalsha(
                    scriptSha,
                    Collections.singletonList(key),
                    Arrays.asList(
                            String.valueOf(capacity),
                            String.valueOf(refillRate),
                            String.valueOf(now),
                            String.valueOf(ttlSeconds)
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
                            (int) capacity,
                            0
                    );
                }
            }
        }

        return new RateLimitResult(
                true,
                (int) capacity,
                0
        );
    }

    public void clearUser(String userId) {
        try (Jedis jedis = pool.getResource()) {
        jedis.del("rate_limit:" + policyName + ":" + userId); 
        } catch (JedisException e) {
            System.err.println("Failed to clear user: " + e.getMessage());
        }
    }

    public void close() {
        pool.close();
    }
}