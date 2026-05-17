package com.example.Config;

import com.example.limiter.redis.token.TokenBucketRateLimiter;
import com.example.limiter.redis.fixed.FixedWindowLimiter;
import com.example.limiter.redis.sliding.SlidingWindowLimiter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import redis.clients.jedis.JedisPool;

@Configuration
public class LimiterConfig {

}