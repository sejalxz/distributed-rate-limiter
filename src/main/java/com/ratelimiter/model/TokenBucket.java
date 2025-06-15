package com.ratelimiter.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Duration;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenBucket {
    private long capacity;
    private long tokens;
    private long lastRefillTime;

    public TokenBucket(long tokens, long lastRefillTime) {
        this.tokens = tokens;
        this.lastRefillTime = lastRefillTime;
    }
}