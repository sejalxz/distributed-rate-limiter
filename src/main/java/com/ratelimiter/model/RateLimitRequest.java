package com.ratelimiter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RateLimitRequest {

    @NotBlank(message = "User ID cannot be blank")
    @JsonProperty("user_id")
    private String userId;

    @NotBlank(message = "Resource cannot be blank")
    private String resource;

    @JsonProperty("algorithm")
    private String algorithm; // Optional: token_bucket, sliding_window, fixed_window

    @JsonProperty("client_ip")
    private String clientIp; // Optional: for IP-based rate limiting

    private String identifier;
    private RateLimitConfig config;

    public String getAlgorithm() {
        return algorithm != null ? algorithm : "token_bucket";
    }
}