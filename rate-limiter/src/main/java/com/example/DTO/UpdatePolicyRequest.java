package com.example.DTO;

import jakarta.validation.constraints.*;

public class UpdatePolicyRequest {

    @Min(
        value = 1,
        message =
        "Max requests must be > 0"
    )
    private Integer maxRequests;

    @Min(
        value = 1,
        message =
        "Window seconds must be > 0"
    )
    private Integer windowSeconds;

    @Positive(
        message =
        "Refill rate must be positive"
    )
    private Double refillRate;

    public Integer getMaxRequests() {
        return maxRequests;
    }

    public void setMaxRequests(
            Integer maxRequests
    ) {
        this.maxRequests =
                maxRequests;
    }

    public Integer getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(
            Integer windowSeconds
    ) {
        this.windowSeconds =
                windowSeconds;
    }

    public Double getRefillRate() {
        return refillRate;
    }

    public void setRefillRate(
            Double refillRate
    ) {
        this.refillRate =
                refillRate;
    }
}