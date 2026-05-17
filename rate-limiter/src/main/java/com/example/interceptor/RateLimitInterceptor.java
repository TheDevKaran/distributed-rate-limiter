package com.example.interceptor;

import com.example.DTO.RateLimitResult;
import com.example.Service.MetricsService;
import com.example.Service.RateLimiterRegistry;
import com.example.annotation.RateLimit;
import com.example.limiter.RateLimiter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterRegistry registry;
    private final MetricsService metrics;

    public RateLimitInterceptor(RateLimiterRegistry registry, MetricsService metrics) {
        this.registry = registry;
        this.metrics = metrics;
        }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {

        String path = request.getRequestURI();

        if (path.startsWith("/admin") || path.startsWith("/health")) {
                return true;
        }

        if (!(handler instanceof HandlerMethod)) {
                return true;
        }
        
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit rateLimit = handlerMethod.getMethod().getAnnotation(RateLimit.class);

        String policy = "fixed";

        if (rateLimit != null) {
            policy = rateLimit.policy();
        }

        System.out.println(
        "Policy used: " + policy
    );

        String clientId =
                request.getHeader("client-id");

        if (clientId == null || clientId.isBlank()) {
            clientId = "anonymous";
        }

        // String path = request.getRequestURI();

        // RateLimitResult result;

        // FixedWindowService selectedService = policyMap.getOrDefault(path, fixedWindowService);

        // result = selectedService.allowRequest(clientId);
        RateLimiter limiter = registry.getLimiter(policy);

        RateLimitResult result = limiter.allowRequest(clientId);
        metrics.request(policy);
        
        request.setAttribute(
            "rateLimitResult",
            result
        );

        response.setHeader(
                "X-RateLimit-Remaining",
                String.valueOf(
                        result.getRemainingRequests()
                )
        );

        response.setHeader(
                "Retry-After",
                String.valueOf(
                        result.getRetryAfterSeconds()
                )
        );

        if (!result.isAllowed()) {
                metrics.blocked();

            response.setStatus(
                    HttpStatus.TOO_MANY_REQUESTS.value()
            );

            response.setContentType("application/json");

            response.setStatus(
                    HttpStatus.TOO_MANY_REQUESTS.value()
            );

            String jsonResponse = """
            {
              "allowed": false,
              "remainingRequests": %d,
              "retryAfterSeconds": %d
            }
            """.formatted(
                    result.getRemainingRequests(),
                    result.getRetryAfterSeconds()
            );
      

            response.getWriter().write(jsonResponse);


            return false;
        }

        return true;
    }
}