package com.ratelimiter.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RateLimiterPerformanceTest {
    
    @Autowired
    private RateLimiterService rateLimiterService;
    
    @Test
    void testPerformanceBenchmark() {
        String identifier = "perf:test:user";
        int iterations = 1000;
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            rateLimiterService.checkRateLimit(identifier + ":" + i);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double requestsPerSecond = (double) iterations / (duration / 1000.0);
        
        System.out.println("Performance Test Results:");
        System.out.println("Total requests: " + iterations);
        System.out.println("Duration: " + duration + "ms");
        System.out.println("Requests/second: " + String.format("%.2f", requestsPerSecond));
        
        // Should handle at least 100 requests per second
        assertTrue(requestsPerSecond > 100, "Performance too slow: " + requestsPerSecond + " req/s");
    }
} 