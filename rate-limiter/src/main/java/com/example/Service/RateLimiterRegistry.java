package com.example.Service;

import com.example.Entity.RateLimitPolicy;
import com.example.Repository.RateLimitPolicyRepository;
import com.example.limiter.RateLimiter;
import com.example.limiter.redis.sliding.SlidingWindowLimiter;
import redis.clients.jedis.JedisPool;
import org.springframework.stereotype.Component;
import com.example.limiter.redis.fixed.FixedWindowLimiter;
import com.example.limiter.redis.token.TokenBucketRateLimiter;

@Component
public class RateLimiterRegistry {

    private final RateLimitPolicyRepository repository;
    private final JedisPool jedisPool;

    public RateLimiterRegistry(

        RateLimitPolicyRepository repository,
        
        JedisPool jedisPool
    ) {

        this.repository = repository;
        this.jedisPool = jedisPool;
    }

    public RateLimiter getLimiter(String policy) {

    RateLimitPolicy config =
            repository.findByPolicyName(policy)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Policy not found: " + policy
                            )
                    );
    System.out.println(config.getMaxRequests());

    return switch (config.getAlgorithm()) {

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
}
}