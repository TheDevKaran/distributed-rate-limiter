package com.example.Service;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MetricsService {

    private final MeterRegistry registry;

    public MetricsService(
            MeterRegistry registry
    ) {

        this.registry =
                registry;
    }

    public void request(
            String policy,

            String clientId
    ) {

        Counter.builder(
                "rate_limiter_requests_total"
        )
        .tag(
                "policy",
                policy
        )
        .tag(
                "client",
                clientId
        )
        .register(registry)
        .increment();
    }

    public void blocked() {

        Counter.builder(
                "rate_limiter_blocked_total"
        )
        .register(registry)
        .increment();
    }

    public Map<String, Object>
    getMetrics() {

        return Map.of(
            "message",
            "Use /actuator/prometheus for metrics"
        );
    }
}