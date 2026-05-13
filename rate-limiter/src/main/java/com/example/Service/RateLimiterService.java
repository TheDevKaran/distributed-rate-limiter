package com.example.Service;

import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    public boolean allowRequest(String clientId) {

        System.out.println("Checking rate limit for: " + clientId);

        return true;
    }
}