package com.example.Repository;

import com.example.Entity.RateLimitPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RateLimitPolicyRepository
        extends JpaRepository<RateLimitPolicy, Long> {

    Optional<RateLimitPolicy> findByPolicyName(
            String policyName
    );
}