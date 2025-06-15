package com.ratelimiter.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RateLimitResult {
    private final boolean allowed;
    private final int remaining;
    private final long resetTime;
    private final String identifier;

    public static RateLimitResult allowed(int remaining, long resetTime, String identifier) {
        return new RateLimitResult(true, remaining, resetTime, identifier);
    }

    public static RateLimitResult denied(long resetTime, String identifier) {
        return new RateLimitResult(false, 0, resetTime, identifier);
    }

    public boolean isAllowed() { return allowed; }
    public int getRemaining() { return remaining; }
    public long getResetTime() { return resetTime; }
    public String getIdentifier() { return identifier; }
}
