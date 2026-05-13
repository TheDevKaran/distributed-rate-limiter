package com.example.DTO;

public class RateLimitResult {

    private final boolean allowed;
    private final long remainingRequests;
    private final long retryAfterSeconds;

    public RateLimitResult(
            boolean allowed,
            long remainingRequests,
            long retryAfterSeconds
    ) {
        this.allowed = allowed;
        this.remainingRequests = remainingRequests;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public long getRemainingRequests() {
        return remainingRequests;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}