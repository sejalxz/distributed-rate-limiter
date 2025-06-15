package com.ratelimiter.service;

import com.ratelimiter.model.RateLimitConfig;
import com.ratelimiter.model.RateLimitMetrics;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.model.TokenBucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Slf4j
@Service
public class RateLimiterService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private static final String RATE_LIMIT_SCRIPT = """
            local key = KEYS[1]
            local maxTokens = tonumber(ARGV[1])
            local refillRate = tonumber(ARGV[2])
            local refillTime = tonumber(ARGV[3])
            local currentTime = tonumber(ARGV[4])
            
            local lastRefillTime = tonumber(redis.call('hget', key, 'lastRefillTime') or currentTime)
            local currentTokens = tonumber(redis.call('hget', key, 'tokens') or maxTokens)
            
            local timePassed = currentTime - lastRefillTime
            local tokensToAdd = math.floor(timePassed * refillRate / refillTime)
            local newTokens = math.min(maxTokens, currentTokens + tokensToAdd)
            
            if newTokens < 1 then
                return 0
            end
            
            redis.call('hset', key, 'tokens', newTokens - 1)
            redis.call('hset', key, 'lastRefillTime', currentTime)
            redis.call('expire', key, 60)
            
            return 1
            """;

    private final DefaultRedisScript<Long> rateLimitScript = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);

    public RateLimitResult checkRateLimit(String identifier) {
        return checkRateLimit(identifier, new RateLimitConfig());
    }

    public RateLimitResult checkRateLimit(String identifier, RateLimitConfig config) {
        boolean allowed = tryAcquire(identifier, config.getCapacity(), config.getRefillRate(), config.getWindow());
        long currentTime = System.currentTimeMillis();
        long resetTime = currentTime + (config.getWindow() * 1000);
        
        return new RateLimitResult(allowed, allowed ? 1 : 0, resetTime, identifier);
    }

    public void resetRateLimit(String identifier) {
        String redisKey = "rate_limit:" + identifier;
        redisTemplate.delete(redisKey);
    }

    public Optional<TokenBucket> getRateLimitStatus(String identifier) {
        String redisKey = "rate_limit:" + identifier;
        String tokens = redisTemplate.opsForHash().get(redisKey, "tokens").toString();
        String lastRefillTime = redisTemplate.opsForHash().get(redisKey, "lastRefillTime").toString();
        
        if (tokens != null && lastRefillTime != null) {
            return Optional.of(new TokenBucket(Long.parseLong(tokens), Long.parseLong(lastRefillTime)));
        }
        
        return Optional.empty();
    }

    private boolean tryAcquire(String key, int maxTokens, int refillRate, int refillTimeSeconds) {
        String redisKey = "rate_limit:" + key;
        long currentTime = System.currentTimeMillis();
        
        Long result = redisTemplate.execute(
            rateLimitScript,
            Collections.singletonList(redisKey),
            String.valueOf(maxTokens),
            String.valueOf(refillRate),
            String.valueOf(refillTimeSeconds),
            String.valueOf(currentTime)
        );
        
        return result != null && result == 1;
    }

    public RateLimitMetrics getMetrics() {
        // Implementation needed
        throw new UnsupportedOperationException("Method not implemented");
    }
}
