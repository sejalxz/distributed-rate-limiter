package com.ratelimiter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

    private int defaultCapacity = 100;
    private long defaultRefillRate = 10;
    private long defaultRefillPeriodMs = 1000;
    private String redisHost = "localhost";
    private int redisPort = 6379;
    private boolean enabled = true;

    // You can add more configuration properties as needed
    // Spring will automatically map from application.yml/properties
}