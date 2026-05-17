package com.example.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "rate_limit_policies")
public class RateLimitPolicy {

    @Id
    @GeneratedValue(
        strategy =
        GenerationType.IDENTITY
    )
    private Long id;

    @Column(
        unique = true,
        nullable = false
    )
    @NotBlank(
        message =
        "Policy name required"
    )
    private String policyName;

    @Enumerated(
        EnumType.STRING
    )
    @NotNull(
        message =
        "Algorithm required"
    )
    @Column(nullable = false)
    private AlgorithmType algorithm;

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

    public RateLimitPolicy() {
    }

    public Long getId() {
        return id;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(
            String policyName
    ) {
        this.policyName =
                policyName;
    }

    public AlgorithmType
    getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(
            AlgorithmType algorithm
    ) {
        this.algorithm =
                algorithm;
    }

    public Integer
    getMaxRequests() {
        return maxRequests;
    }

    public void setMaxRequests(
            Integer maxRequests
    ) {
        this.maxRequests =
                maxRequests;
    }

    public Integer
    getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(
            Integer windowSeconds
    ) {
        this.windowSeconds =
                windowSeconds;
    }

    public Double
    getRefillRate() {
        return refillRate;
    }

    public void setRefillRate(
            Double refillRate
    ) {
        this.refillRate =
                refillRate;
    }
}