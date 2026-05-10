package com.example.inmemory.token;


import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

public class TokenBucketRateLimiter {
    Map<String, Bucket> mpp;
    long capacity;
    double refillRate;
    private final LongSupplier clock;                          // 1. add field

    public TokenBucketRateLimiter(long capacity, double refillRate){
        this(capacity, refillRate, System::currentTimeMillis);
    }

    public TokenBucketRateLimiter(long capacity, double refillRate, LongSupplier clock){
        this.capacity = capacity;
        this.refillRate = refillRate; 
        this.mpp = new HashMap<>();   
        this.clock = clock;
    }

    public boolean allowRequests(String userID){
        if (capacity <= 0) return false;

        long now = clock.getAsLong();  

        if(!mpp.containsKey(userID)){
            double tokens = capacity;
            long lastRefillTime = now;

            Bucket b = new Bucket(tokens, lastRefillTime);
            mpp.put(userID, b);

            b.tokens--;  // consume
            return true;
        }

        Bucket pick = mpp.get(userID);

        double timePassed = (now - pick.lastRefillTime) / 1000.0;
        double tokensToAdd = timePassed * refillRate;   

        pick.tokens = Math.min(capacity, pick.tokens + tokensToAdd);
        pick.lastRefillTime = now;

        if(pick.tokens >= 1){
            pick.tokens--;
            return true;
        }

        return false;
    }
}