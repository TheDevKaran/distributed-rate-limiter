package com.example.Service;

import com.example.Entity.RateLimitPolicy;
import com.example.Exception.PolicyNotFoundException;
import com.example.Exception.UnsupportedAlgorithmException;
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

        System.out.println(
            "Returned from cache"
        );

        return cache.get(policy);
    }

    System.out.println(
        "Fetching policy"
    );

    RateLimitPolicy config =
            repository.findByPolicyName(policy)
                    .orElseThrow(() ->
                        new PolicyNotFoundException(
                            policy
                        )
                    );

    System.out.println(
        config.getAlgorithm()
    );

    System.out.println(
        config.getMaxRequests()
    );

    RateLimiter limiter = switch (
            config.getAlgorithm()
    ) {

        case FIXED -> {

            System.out.println(
                "Creating FIXED"
            );

            yield new FixedWindowLimiter(
                    jedisPool,
                    config.getMaxRequests(),
                    config.getWindowSeconds(),
                    policy
            );
        }

        case SLIDING -> {

            System.out.println(
                "Creating SLIDING"
            );

            yield new SlidingWindowLimiter(
                    jedisPool,
                    config.getMaxRequests(),
                    config.getWindowSeconds(),
                    policy
            );
        }

        case TOKEN -> {

            System.out.println(
                "Creating TOKEN"
            );

            yield new TokenBucketRateLimiter(
                    jedisPool,
                    config.getMaxRequests(),
                    config.getRefillRate(),
                    policy
            );
        }

        default ->
            throw new UnsupportedAlgorithmException(
                config.getAlgorithm()
        );
    };

    System.out.println(
        "Limiter created"
    );

    cache.put(policy, limiter);

    cacheTime.put(
            policy,
            System.currentTimeMillis()
    );

    return limiter;
}
}