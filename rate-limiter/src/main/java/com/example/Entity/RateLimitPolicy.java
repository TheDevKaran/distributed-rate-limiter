package com.example.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "rate_limit_policies")
public class RateLimitPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String policyName;

    @Column(nullable = false)
    private String algorithm;

    private Integer maxRequests;

    private Integer windowSeconds;

    private Double refillRate;

    public RateLimitPolicy() {
    }

    public Long getId() {
        return id;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public Integer getMaxRequests() {
        return maxRequests;
    }

    public void setMaxRequests(Integer maxRequests) {
        this.maxRequests = maxRequests;
    }

    public Integer getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(Integer windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public Double getRefillRate() {
        return refillRate;
    }

    public void setRefillRate(Double refillRate) {
        this.refillRate = refillRate;
    }
}