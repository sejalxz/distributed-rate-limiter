package com.ratelimiter.service;

import com.ratelimiter.model.RateLimitConfig;
import com.ratelimiter.model.RateLimitResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RateLimiterLoadTest {
    
    @Autowired
    private RateLimiterService rateLimiterService;
    
    @Test
    void testConcurrentRequests() throws InterruptedException {
        String identifier = "load:test:user";
        int threadCount = 10;
        int requestsPerThread = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger deniedCount = new AtomicInteger(0);
        
        RateLimitConfig config = new RateLimitConfig(10, 1, 1);
        
        // Create multiple threads making concurrent requests
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        RateLimitResult result = rateLimiterService.checkRateLimit(identifier, config);
                        if (result.isAllowed()) {
                            allowedCount.incrementAndGet();
                        } else {
                            deniedCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await(10, TimeUnit.SECONDS);
        
        int totalRequests = allowedCount.get() + deniedCount.get();
        assertEquals(threadCount * requestsPerThread, totalRequests);
        
        // Should allow exactly 10 requests (capacity)
        assertEquals(10, allowedCount.get());
        assertEquals(40, deniedCount.get());
    }
} 