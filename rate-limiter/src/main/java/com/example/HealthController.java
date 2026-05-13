package com.example;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.annotation.RateLimit;

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
}