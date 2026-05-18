package com.example.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final Counter total;
    private final Counter blocked;

    public MetricsService(
            MeterRegistry registry
    ) {

        total = registry.counter(
                "rate_limiter_requests_total"
        );

        blocked = registry.counter(
                "rate_limiter_blocked_total"
        );
    }

    public void request(
            String policy,
            String clientId
    ) {
        total.increment();
    }

    public void blocked() {
        blocked.increment();
    }

    public Map<String,Object> getMetrics() {

    Map<String,Object> ans =
        new HashMap<>();

    ans.put(
        "totalRequests",
        total.count()
    );

    ans.put(
        "blockedRequests",
        blocked.count()
    );

    return ans;
}
}