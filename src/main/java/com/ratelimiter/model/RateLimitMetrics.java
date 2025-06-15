package com.ratelimiter.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class RateLimitMetrics {
    private long totalRequests;
    private long allowedRequests;
    private long deniedRequests;
    private double allowedRate;
    private long activeKeys;
    private Map<String, Long> topRateLimitedKeys;
    private long lastResetTime;

    public double getDeniedRate() {
        return totalRequests > 0 ? (double) deniedRequests / totalRequests : 0.0;
    }
}