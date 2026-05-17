package com.example.Controller;

import com.example.Entity.RateLimitPolicy;
import com.example.Repository.RateLimitPolicyRepository;
import com.example.Service.MetricsService;

import java.util.Map;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final RateLimitPolicyRepository repository;
    private final MetricsService metricsService;

    public AdminController(RateLimitPolicyRepository repository, MetricsService metricsService) {
        this.repository = repository;
        this.metricsService = metricsService;
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

    @GetMapping("/metrics")
    public Map<String,Object>
    metrics() {

        return metricsService
                .getMetrics();
    }
}