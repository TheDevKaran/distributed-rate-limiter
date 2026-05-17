package com.example.Config;

import com.example.limiter.redis.token.TokenBucketRateLimiter;
import com.example.limiter.redis.fixed.FixedWindowLimiter;
import com.example.limiter.redis.sliding.SlidingWindowLimiter;
import com.example.Service.FixedWindowService;
import com.example.Service.SlidingWindowService;
import com.example.Service.TokenBucketService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import redis.clients.jedis.JedisPool;

@Configuration
public class LimiterConfig {

    @Bean
    public FixedWindowLimiter defaultLimiter(JedisPool jedisPool) {
        return new FixedWindowLimiter(
                jedisPool,
                5,
                60,
                "default"
        );
    }

    @Bean
    public FixedWindowLimiter strictLimiter(JedisPool jedisPool) {
        return new FixedWindowLimiter(
                jedisPool,
                3,
                60,
                "strict"
        );
    }

    @Bean
    public SlidingWindowLimiter slidingLimiter(JedisPool jedisPool) {
        return new SlidingWindowLimiter(
            jedisPool,
            5,
            60,
            "sliding"
        );
    }

    @Bean
    public TokenBucketRateLimiter tokenBucketLimiter(JedisPool jedisPool) {
        return new TokenBucketRateLimiter(
                jedisPool,
                5,
                1.0,
                "token"
        );
    }

    @Bean
    public FixedWindowService fixedWindowService(@Qualifier("defaultLimiter") FixedWindowLimiter limiter) {
        return new FixedWindowService(limiter);
    }

    @Bean
    public FixedWindowService strictLimiterService(@Qualifier("strictLimiter") FixedWindowLimiter limiter) {
        return new FixedWindowService(limiter);
    }   

    @Bean
    public SlidingWindowService slidingWindowService(SlidingWindowLimiter limiter) {
        return new SlidingWindowService(limiter);
    }

    @Bean
    public TokenBucketService tokenBucketService(
            TokenBucketRateLimiter limiter
    ) {
        return new TokenBucketService(limiter);
    }
}