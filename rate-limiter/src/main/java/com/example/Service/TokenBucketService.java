package com.example.Service;

import com.example.DTO.RateLimitResult;
import com.example.limiter.redis.token.TokenBucketRateLimiter;

import org.springframework.stereotype.Service;

@Service
public class TokenBucketService implements RateLimiterService {

    private final TokenBucketRateLimiter limiter;

    public TokenBucketService(TokenBucketRateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    public RateLimitResult allowRequest(String userId) {
        return limiter.allowRequest(userId);
  
}
}