package com.example.Service;

import com.example.DTO.RateLimitResult;
import com.example.limiter.redis.sliding.SlidingWindowLimiter;
import org.springframework.stereotype.Service;

@Service
public class SlidingWindowService implements RateLimiterService {

    private final SlidingWindowLimiter limiter;

    public SlidingWindowService(SlidingWindowLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    public RateLimitResult allowRequest(String userId) {
        boolean allowed = limiter.allowedReq(userId);
        return new RateLimitResult(
            allowed,
            allowed ? 1 : 0,
            0
        );
    }   
}