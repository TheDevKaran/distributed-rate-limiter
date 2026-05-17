package com.example.limiter;

import com.example.DTO.RateLimitResult;

public interface RateLimiter {

    RateLimitResult allowRequest(String userId);
}