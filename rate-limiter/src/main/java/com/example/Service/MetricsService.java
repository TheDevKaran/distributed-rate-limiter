package com.example.Service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;

@Service
public class MetricsService {

    private final AtomicLong total =
            new AtomicLong();

    private final AtomicLong blocked =
            new AtomicLong();

    private final Map<String,AtomicLong>
            algoHits =
            new ConcurrentHashMap<>();

    private final Map<String, AtomicLong>
        clientUsage =
        new ConcurrentHashMap<>();

    public void request(String policy, String clientId) {

        total.incrementAndGet();

        algoHits
            .computeIfAbsent(
                policy,
                k -> new AtomicLong()
            )
            .incrementAndGet();

        clientUsage
            .computeIfAbsent(
                    clientId,
                    k -> new AtomicLong()
            )
            .incrementAndGet();
    }

    public void blocked() {
        blocked.incrementAndGet();
    }

    public Map<String,Object> getMetrics() {

        return Map.of(
            "totalRequests",
            total.get(),

            "blockedRequests",
            blocked.get(),

            "algorithmUsage",
            algoHits,

            "clientUsage",
            clientUsage
        );
    }
}