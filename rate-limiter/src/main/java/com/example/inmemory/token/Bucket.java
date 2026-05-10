package com.example.inmemory.token;

public class Bucket {
    double tokens;
    long lastRefillTime;

    Bucket(double tokens, long lastRefillTime){
        this.tokens = tokens;
        this.lastRefillTime = lastRefillTime;
    }
}
