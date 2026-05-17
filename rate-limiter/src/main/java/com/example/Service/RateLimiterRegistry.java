package com.example.Service;

import com.example.limiter.RateLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RateLimiterRegistry {

    private final Map<String, RateLimiter> policyMap;

    public RateLimiterRegistry(

            @Qualifier("defaultLimiter")
            RateLimiter defaultLimiter,

            @Qualifier("strictLimiter")
            RateLimiter strictLimiter,

            @Qualifier("slidingLimiter")
            RateLimiter slidingLimiter,

            @Qualifier("tokenBucketLimiter")
            RateLimiter tokenLimiter
    ) {

        this.policyMap = Map.of(
                "default", defaultLimiter,
                "strict", strictLimiter,
                "sliding", slidingLimiter,
                "token", tokenLimiter
        );
    }

    public RateLimiter getLimiter(String policy) {

        return policyMap.getOrDefault(
                policy,
                policyMap.get("default")
        );
    }
}