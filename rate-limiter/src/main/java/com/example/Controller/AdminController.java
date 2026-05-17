package com.example.Controller;

import com.example.Entity.RateLimitPolicy;
import com.example.Repository.RateLimitPolicyRepository;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final RateLimitPolicyRepository repository;

    public AdminController(RateLimitPolicyRepository repository) {
        this.repository = repository;
    }

    @PutMapping("/policy/{name}")
    public RateLimitPolicy updatePolicy(

            @PathVariable String name,

            @RequestBody RateLimitPolicy body
    ) {

        RateLimitPolicy policy =
                repository.findByPolicyName(name)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Policy not found"
                                )
                        );

        if (body.getMaxRequests() != null)
            policy.setMaxRequests(
                    body.getMaxRequests()
            );

        if (body.getWindowSeconds() != null)
            policy.setWindowSeconds(
                    body.getWindowSeconds()
            );

        if (body.getRefillRate() != null)
            policy.setRefillRate(
                    body.getRefillRate()
            );

        return repository.save(policy);
    }
}