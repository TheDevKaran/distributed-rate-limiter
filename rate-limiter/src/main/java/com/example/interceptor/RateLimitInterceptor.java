package com.example.interceptor;

import com.example.DTO.RateLimitResult;
import com.example.Service.FixedWindowService;
import com.example.Service.RateLimiterService;
import com.example.annotation.RateLimit;

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

    private final RateLimiterService fixedWindowService;
    private final RateLimiterService strictLimiterService;
    private final RateLimiterService slidingWindowService;
    private final Map<String, RateLimiterService> policyMap;

    public RateLimitInterceptor(

    @Qualifier("fixedWindowService")
    RateLimiterService fixedWindowService,

    @Qualifier("strictLimiterService")
    RateLimiterService strictLimiterService,

    @Qualifier("slidingWindowService")
    RateLimiterService slidingWindowService
)
    {
            this.fixedWindowService = fixedWindowService;
            this.strictLimiterService = strictLimiterService;
            this.slidingWindowService = slidingWindowService;
            this.policyMap = Map.of(
                "strict", strictLimiterService,
                "default", fixedWindowService,
                "sliding", slidingWindowService

        );
        }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {
        
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit rateLimit = handlerMethod.getMethod().getAnnotation(RateLimit.class);

        String policy = "default";

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
        RateLimiterService selectedService = policyMap.getOrDefault(policy,fixedWindowService);
        RateLimitResult result = selectedService.allowRequest(clientId);
        
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