package com.ratelimiter.controller;

import com.ratelimiter.model.RateLimitRequest;
import com.ratelimiter.model.RateLimitResponse;
import com.ratelimiter.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

public class RateLimiterController {

    private final RateLimiterService rateLimiterService;

    public RateLimiterController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }


    @PostMapping("/check")
    public ResponseEntity<RateLimitResponse> checkRateLimit(@RequestBody RateLimitRequest request) {
        System.out.println("Checking rate limit for identifier: " + request.getIdentifier());
        
        var result = request.getConfig() != null ?
            rateLimiterService.checkRateLimit(request.getIdentifier(), request.getConfig()) :
            rateLimiterService.checkRateLimit(request.getIdentifier());
        
        if (result.isAllowed()) {
            System.out.println("Rate limit check passed for identifier: " + request.getIdentifier());
            return ResponseEntity.ok(new RateLimitResponse(
                result.isAllowed(),
                result.getRemaining(),
                result.getResetTime(),
                result.getIdentifier()
            ));
        } else {
            System.out.println("Rate limit exceeded for identifier: " + request.getIdentifier());
            return ResponseEntity.status(429).body(new RateLimitResponse(
                result.isAllowed(),
                result.getRemaining(),
                result.getResetTime(),
                result.getIdentifier()
            ));
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> resetRateLimit(@RequestBody RateLimitRequest request) {
        System.out.println("Resetting rate limit for identifier: " + request.getIdentifier());
        rateLimiterService.resetRateLimit(request.getIdentifier());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status/{identifier}")
    public ResponseEntity<RateLimitResponse> getRateLimitStatus(@PathVariable String identifier) {
        System.out.println("Getting rate limit status for identifier: " + identifier);
        var result = rateLimiterService.checkRateLimit(identifier);
        
        return ResponseEntity.ok(new RateLimitResponse(
            result.isAllowed(),
            result.getRemaining(),
            result.getResetTime(),
            result.getIdentifier()
        ));
    }
}