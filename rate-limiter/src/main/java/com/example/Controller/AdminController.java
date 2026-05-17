package com.example.Controller;

import com.example.DTO.UpdatePolicyRequest;
import com.example.Entity.RateLimitPolicy;
import com.example.Exception.PolicyNotFoundException;
import com.example.Repository.RateLimitPolicyRepository;
import com.example.Service.MetricsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.Map;

import org.springframework.web.bind.annotation.*;

@Tag(
    name =
    "Admin APIs"
)
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final RateLimitPolicyRepository repository;
    private final MetricsService metricsService;

    public AdminController(RateLimitPolicyRepository repository, MetricsService metricsService) {
        this.repository = repository;
        this.metricsService = metricsService;
    }

    @Operation(
        summary =
        "Update rate limit policy"
    )
    @PutMapping("/policy/{name}")
    public RateLimitPolicy updatePolicy(

            @PathVariable String name,

            @RequestBody
            @Valid
            UpdatePolicyRequest body
    ) {

        RateLimitPolicy policy =
                repository.findByPolicyName(name)
                        .orElseThrow(
                            () ->
                                new PolicyNotFoundException(
                                    name
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