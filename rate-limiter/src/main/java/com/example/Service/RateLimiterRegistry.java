package com.example.Service;

import com.example.Entity.RateLimitPolicy;
import com.example.Repository.RateLimitPolicyRepository;
import com.example.limiter.RateLimiter;
import com.example.limiter.redis.sliding.SlidingWindowLimiter;
import redis.clients.jedis.JedisPool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import com.example.limiter.redis.fixed.FixedWindowLimiter;
import com.example.limiter.redis.token.TokenBucketRateLimiter;

@Component
public class RateLimiterRegistry {

    private final RateLimitPolicyRepository repository;
    private final JedisPool jedisPool;
    private final Map<String, RateLimiter> cache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTime =
        new ConcurrentHashMap<>();

    private static final long CACHE_TTL = 60_000;
    public RateLimiterRegistry(

        RateLimitPolicyRepository repository,
        
        JedisPool jedisPool
    ) {

        this.repository = repository;
        this.jedisPool = jedisPool;
    }

    public RateLimiter getLimiter(String policy) {

        Long time = cacheTime.get(policy);

            if (
                cache.containsKey(policy)
                && time != null
                && System.currentTimeMillis() - time < CACHE_TTL
            ) {
                return cache.get(policy);
            }

    RateLimitPolicy config =
            repository.findByPolicyName(policy)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Policy not found: " + policy
                            )
                    );
    System.out.println(config.getMaxRequests());

    RateLimiter limiter = switch (config.getAlgorithm()) {

        case "fixed" ->

                new FixedWindowLimiter(
                        jedisPool,
                        config.getMaxRequests(),
                        config.getWindowSeconds(),
                        policy
                );

        case "sliding" ->

                new SlidingWindowLimiter(
                        jedisPool,
                        config.getMaxRequests(),
                        config.getWindowSeconds(),
                        policy
                );

        case "token" ->

                new TokenBucketRateLimiter(
                        jedisPool,
                        config.getMaxRequests(),
                        config.getRefillRate(),
                        policy
                );

        default -> throw new RuntimeException(
                "Unknown algorithm: "
                + config.getAlgorithm()
        );
    };
    cache.put(policy, limiter);
    cacheTime.put(policy,System.currentTimeMillis());

    return limiter;
    }
}