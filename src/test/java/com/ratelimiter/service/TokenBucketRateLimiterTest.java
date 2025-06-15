package com.ratelimiter.service;

import com.ratelimiter.config.RateLimiterTestConfig;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.model.TokenBucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TokenBucketRateLimiterTest {
    
    @Autowired
    private TokenBucketRateLimiter rateLimiter;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @BeforeEach
    void setUp() {
        // Clean up Redis before each test
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }
    
    @Test
    @Order(1)
    void testBasicRateLimiting() {
        String identifier = "user:123";
        
        // First request should be allowed
        RateLimitResult result1 = rateLimiter.isAllowed(identifier, 5, 1, Duration.ofSeconds(1));
        assertTrue(result1.isAllowed());
        assertEquals(4, result1.getRemaining());
        
        // Consume all tokens
        for (int i = 0; i < 4; i++) {
            RateLimitResult result = rateLimiter.isAllowed(identifier, 5, 1, Duration.ofSeconds(1));
            assertTrue(result.isAllowed());
        }
        
        // Next request should be denied
        RateLimitResult result2 = rateLimiter.isAllowed(identifier, 5, 1, Duration.ofSeconds(1));
        assertFalse(result2.isAllowed());
        assertEquals(0, result2.getRemaining());
    }
    
    @Test
    @Order(2)
    void testTokenRefill() throws InterruptedException {
        String identifier = "user:refill:test";
        
        // Consume all tokens
        for (int i = 0; i < 3; i++) {
            rateLimiter.isAllowed(identifier, 3, 3, Duration.ofSeconds(1));
        }
        
        // Should be denied
        RateLimitResult denied = rateLimiter.isAllowed(identifier, 3, 3, Duration.ofSeconds(1));
        assertFalse(denied.isAllowed());
        
        // Wait for refill
        Thread.sleep(1100);
        
        // Should be allowed again
        RateLimitResult allowed = rateLimiter.isAllowed(identifier, 3, 3, Duration.ofSeconds(1));
        assertTrue(allowed.isAllowed());
    }
    
    @Test
    @Order(3)
    void testDifferentIdentifiers() {
        String user1 = "user:1";
        String user2 = "user:2";
        
        // Both users should have separate buckets
        RateLimitResult result1 = rateLimiter.isAllowed(user1, 2, 1, Duration.ofSeconds(1));
        RateLimitResult result2 = rateLimiter.isAllowed(user2, 2, 1, Duration.ofSeconds(1));
        
        assertTrue(result1.isAllowed());
        assertTrue(result2.isAllowed());
        assertEquals(1, result1.getRemaining());
        assertEquals(1, result2.getRemaining());
    }
    
    @Test
    @Order(4)
    void testBucketStateRetrieval() {
        String identifier = "user:state:test";
        
        // Make a request to create bucket
        rateLimiter.isAllowed(identifier, 10, 5, Duration.ofMinutes(1));
        
        Optional<TokenBucket> bucket = rateLimiter.getBucketState(identifier);
        assertTrue(bucket.isPresent());
        assertEquals(10, bucket.get().getCapacity());
        assertEquals(9, bucket.get().getTokens()); // One token consumed
    }
    
    @Test
    @Order(5)
    void testRateLimitReset() {
        String identifier = "user:reset:test";
        
        // Consume all tokens
        for (int i = 0; i < 5; i++) {
            rateLimiter.isAllowed(identifier, 5, 1, Duration.ofSeconds(1));
        }
        
        // Should be denied
        RateLimitResult denied = rateLimiter.isAllowed(identifier, 5, 1, Duration.ofSeconds(1));
        assertFalse(denied.isAllowed());
        
        // Reset rate limit
        rateLimiter.resetRateLimit(identifier);
        
        // Should be allowed again
        RateLimitResult allowed = rateLimiter.isAllowed(identifier, 5, 1, Duration.ofSeconds(1));
        assertTrue(allowed.isAllowed());
    }
} 