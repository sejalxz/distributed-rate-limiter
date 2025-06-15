package com.ratelimiter.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitResponse {
    private boolean allowed;
    private long remainingTokens;
    private long resetTime;
    private String identifier;

    public RateLimitResponse(RateLimitResult result) {
        this.allowed = result.isAllowed();
        this.remainingTokens = result.getRemaining(); // Fixed method name
        this.resetTime = result.getResetTime();
        this.identifier = result.getIdentifier();
    }

    // Removed redundant manual getters - Lombok generates them
    // Keep only custom getters if they have different behavior
    public long getRemaining() {
        return remainingTokens;
    }

    public static RateLimitResponse fromResult(RateLimitResult result) {
        return new RateLimitResponse(result);
    }
}