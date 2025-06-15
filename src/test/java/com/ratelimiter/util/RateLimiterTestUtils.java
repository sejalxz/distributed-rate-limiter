package com.ratelimiter.util;

import com.ratelimiter.model.TokenBucket;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class RateLimiterTestUtils {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public RateLimiterTestUtils(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    public void cleanupRedis() {
        try {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
        } catch (Exception e) {
            // Ignore cleanup errors in tests
        }
    }
    
    public void createTestBucket(String identifier, long capacity, long tokens) {
        TokenBucket bucket = new TokenBucket(capacity, tokens, System.currentTimeMillis());
        redisTemplate.opsForValue().set("rate_limit:" + identifier, bucket);
    }
    
    public long getRedisKeyCount(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        return keys != null ? keys.size() : 0;
    }
} 