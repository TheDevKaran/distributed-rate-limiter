package com.example.Service;

import org.springframework.stereotype.Service;

import com.example.DTO.RateLimitResult;
import com.example.limiter.redis.fixed.FixedWindowLimiter;

@Service
public class FixedWindowService implements RateLimiterService {
    
    private final FixedWindowLimiter limiter;

    public FixedWindowService(
            FixedWindowLimiter limiter
    ) {
        this.limiter = limiter;
    }

    public RateLimitResult allowRequest(
            String userId
    ) {

        return limiter.allowedReq(userId);
    }
}