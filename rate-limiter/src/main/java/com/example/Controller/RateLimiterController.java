package com.example.Controller;

import com.example.DTO.CheckRequest;
import com.example.DTO.CheckResponse;
import com.example.Service.FixedWindowService;
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

    @PostMapping("/check")
    public CheckResponse check(
            @RequestBody CheckRequest request
    ) {

        boolean allowed =
                fixedWindowService.allowRequest(
                        request.getClientId()
                );

        return new CheckResponse(allowed);
    }
}