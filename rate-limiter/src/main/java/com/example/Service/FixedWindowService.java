package com.example.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.limiter.redis.fixed.FixedWindowLimiter;

import jakarta.annotation.PostConstruct;
import redis.clients.jedis.JedisPool;

@Service
public class FixedWindowService {
    private final JedisPool jedisPool;

    @Value("${rate.limit.fixed.max-requests}")
    private int maxRequests;

    @Value("${rate.limit.fixed.window-seconds}")
    private int windowSeconds;

    private FixedWindowLimiter limiter;

    public FixedWindowService(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @PostConstruct
    public void init() {

        this.limiter =
                new FixedWindowLimiter(
                        jedisPool,
                        maxRequests,
                        windowSeconds
                );
    }

    public boolean allowRequest(String userId) {
        return limiter.allowedReq(userId);
    }
}