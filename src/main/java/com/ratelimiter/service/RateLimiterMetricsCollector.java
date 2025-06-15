package com.ratelimiter.service;

import com.ratelimiter.model.RateLimitMetrics;
import com.ratelimiter.model.RateLimitResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiterMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    private final RateLimiterService rateLimiterService;
    
    @EventListener
    @Async
    public void handleRateLimitEvent(RateLimitResult result) {
        // Record custom metrics
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("rate_limit_decision_time")
                .description("Time taken to make rate limit decision")
                .register(meterRegistry));
        
        // Record by result type
        meterRegistry.counter("rate_limit_decisions", 
                "result", result.isAllowed() ? "allowed" : "denied",
                "identifier", result.getIdentifier())
                .increment();
    }
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void recordMetrics() {
        try {
            RateLimitMetrics metrics = rateLimiterService.getMetrics();
            
            meterRegistry.gauge("rate_limiter_active_keys_total", metrics.getActiveKeys());
            meterRegistry.gauge("rate_limiter_allowed_rate", metrics.getAllowedRate());
            meterRegistry.gauge("rate_limiter_denied_rate", metrics.getDeniedRate());
            
            log.debug("Recorded rate limiter metrics: {}", metrics);
        } catch (Exception e) {
            log.error("Error recording metrics", e);
        }
    }
} 