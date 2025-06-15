package com.ratelimiter.exception;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {

    private final String userId;
    private final String resource;
    private final long retryAfter;
    private final int currentUsage;
    private final int limit;

    public RateLimitExceededException(String userId, String resource,
                                      long retryAfter, int currentUsage, int limit) {
        super(String.format("Rate limit exceeded for user %s on resource %s. Current: %d, Limit: %d, Retry after: %d seconds",
                userId, resource, currentUsage, limit, retryAfter));
        this.userId = userId;
        this.resource = resource;
        this.retryAfter = retryAfter;
        this.currentUsage = currentUsage;
        this.limit = limit;
    }

    public RateLimitExceededException(String message) {
        super(message);
        this.userId = null;
        this.resource = null;
        this.retryAfter = 0;
        this.currentUsage = 0;
        this.limit = 0;
    }

    public RateLimitExceededException(String message, Throwable cause) {
        super(message, cause);
        this.userId = null;
        this.resource = null;
        this.retryAfter = 0;
        this.currentUsage = 0;
        this.limit = 0;
    }
}