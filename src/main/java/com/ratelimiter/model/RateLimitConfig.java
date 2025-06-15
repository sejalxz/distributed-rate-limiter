package com.ratelimiter.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitConfig {
    private int capacity = 10;
    private int refillRate = 1;
    private int window = 60; // in seconds

    public int getCapacity() { return capacity; }
    public int getRefillRate() { return refillRate; }
    public int getWindow() { return window; }
}