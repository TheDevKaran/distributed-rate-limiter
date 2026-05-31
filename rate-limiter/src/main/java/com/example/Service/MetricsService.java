package com.example.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final MeterRegistry registry;

    private final Counter total;
    private final Counter blocked;
    private final Counter accepted;

    public MetricsService(MeterRegistry registry) {

        this.registry = registry;

        total = Counter.builder("rate_limiter_requests_total")
                .description("Total requests")
                .register(registry);

        blocked = Counter.builder("rate_limiter_blocked_total")
                .description("Blocked requests")
                .register(registry);

        accepted = Counter.builder("rate_limiter_accepted_total")
                .description("Accepted requests")
                .register(registry);
    }

    public void request(String policy, String clientId) {

        total.increment();

        Counter.builder("rate_limiter_requests_by_policy")
                .tag("policy", policy)
                .register(registry)
                .increment();
    }

    public void blocked(String policy) {

        blocked.increment();

        Counter.builder("rate_limiter_blocked_by_policy")
                .tag("policy", policy)
                .register(registry)
                .increment();
    }

    public void accepted(String policy) {

        accepted.increment();

        Counter.builder("rate_limiter_accepted_by_policy")
                .tag("policy", policy)
                .register(registry)
                .increment();
    }

    public Map<String, Object> getMetrics() {

        Map<String, Object> ans = new HashMap<>();

        ans.put("totalRequests", total.count());
        ans.put("blockedRequests", blocked.count());
        ans.put("acceptedRequests", accepted.count());

        return ans;
    }
}