package com.example.Controller;

import com.example.DTO.CheckRequest;
import com.example.DTO.CheckResponse;
import com.example.DTO.RateLimitResult;
import com.example.Service.FixedWindowService;
import com.example.annotation.RateLimit;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rate-limit")
public class RateLimiterController {

    private final FixedWindowService fixedWindowService;

    public RateLimiterController(
            FixedWindowService fixedWindowService
    ) {
        this.fixedWindowService = fixedWindowService;
    }
        
        @RateLimit(policy = "default")
        @GetMapping("/hello")
        public Map<String, Object> hello(
                HttpServletRequest request
        ) {

        RateLimitResult result =
                (RateLimitResult) request.getAttribute(
                        "rateLimitResult"
                );

        return Map.of(
                "allowed", true,
                "remainingRequests",
                result.getRemainingRequests(),
                "retryAfterSeconds",
                result.getRetryAfterSeconds()
        );
        }
}