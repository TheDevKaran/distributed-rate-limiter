package com.example;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.DTO.RateLimitResult;
import com.example.annotation.RateLimit;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(
    name =
    "Rate Limited APIs"
)
@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "Hee Hee";
    }

    @RateLimit(policy = "strict")
    @GetMapping("/login")
    public String login() {
        return "Logged In!";
    }

    @RateLimit(policy = "fixed")
    @GetMapping("/fixed")
    public Map<String, Object> fixed(HttpServletRequest request) {
        RateLimitResult result = (RateLimitResult) request.getAttribute("rateLimitResult");
        return Map.of(
            "allowed", true,
            "remainingRequests",
            result.getRemainingRequests(),
            "retryAfterSeconds",
            result.getRetryAfterSeconds()
        );
    }

    @RateLimit(policy = "sliding")
    @GetMapping("/sliding")
    public Map<String, String> sliding() {
        return Map.of(
            "message",
            "Sliding Window API"
        );
    }

    @RateLimit(policy = "token")
@GetMapping("/token")
public Map<String, String> token() {
    return Map.of(
        "message",
        "Token Bucket API"
    );
}
}