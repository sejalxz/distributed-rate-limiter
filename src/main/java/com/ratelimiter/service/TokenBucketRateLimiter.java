package com.ratelimiter.service;

import com.ratelimiter.config.RateLimiterProperties;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.model.TokenBucket;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TokenBucketRateLimiter {

    private final RedisTemplate<String, String> redisTemplate;
    private final RateLimiterProperties properties;
    private final Set<String> activeKeys = ConcurrentHashMap.newKeySet();

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

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    /**
     * Check if request is allowed based on rate limiting rules
     */
    public RateLimitResult isAllowed(String identifier) {
        return isAllowed(identifier,
                properties.getDefaultCapacity(),
                properties.getDefaultRefillRate(),
                Duration.ofMillis(properties.getDefaultRefillPeriodMs()));
    }

    /**
     * Check if request is allowed with custom parameters
     */
    public RateLimitResult isAllowed(String identifier, long capacity, long refillRate, Duration window) {
        String key = RATE_LIMIT_KEY_PREFIX + identifier;
        long currentTime = System.currentTimeMillis();

        Long result = redisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(key),
                String.valueOf(capacity),
                String.valueOf(refillRate),
                String.valueOf(window.toMillis()),
                String.valueOf(currentTime)
        );

        boolean allowed = result != null && result == 1;
        long resetTime = currentTime + window.toMillis();

        if (allowed) {
            activeKeys.add(key);
            return RateLimitResult.allowed(1, resetTime, identifier);
        } else {
            return RateLimitResult.denied(resetTime, identifier);
        }
    }

    /**
     * Get current bucket state for monitoring
     */
    public Optional<TokenBucket> getBucketState(String identifier) {
        String key = RATE_LIMIT_KEY_PREFIX + identifier;
        Object tokensObj = redisTemplate.opsForHash().get(key, "tokens");
        Object lastRefillTimeObj = redisTemplate.opsForHash().get(key, "lastRefillTime");

        if (tokensObj != null && lastRefillTimeObj != null) {
            try {
                long tokens = Long.parseLong(tokensObj.toString());
                long lastRefillTime = Long.parseLong(lastRefillTimeObj.toString());
                return Optional.of(new TokenBucket(tokens, lastRefillTime));
            } catch (NumberFormatException e) {
                System.err.println("Error parsing bucket state for " + identifier + ": " + e.getMessage());
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    /**
     * Reset rate limit for specific identifier
     */
    public void resetRateLimit(String identifier) {
        String key = RATE_LIMIT_KEY_PREFIX + identifier;
        redisTemplate.delete(key);
        activeKeys.remove(key);
        System.out.println("Rate limit reset for identifier: " + identifier);
    }

    /**
     * Get all active rate limit keys (for monitoring)
     */
    public Set<String> getActiveRateLimitKeys() {
        return Collections.unmodifiableSet(activeKeys);
    }
}