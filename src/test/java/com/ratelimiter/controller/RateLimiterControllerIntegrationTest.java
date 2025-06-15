package com.ratelimiter.controller;

import com.ratelimiter.model.RateLimitConfig;
import com.ratelimiter.model.RateLimitMetrics;
import com.ratelimiter.model.RateLimitRequest;
import com.ratelimiter.model.RateLimitResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
class RateLimiterControllerIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1/rate-limiter";
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void testCheckRateLimitEndpoint() {
        RateLimitRequest request = new RateLimitRequest();
        request.setIdentifier("integration:user:1");

        ResponseEntity<RateLimitResponse> response = restTemplate.postForEntity(
                baseUrl + "/check", request, RateLimitResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isAllowed());
        assertNotNull(response.getHeaders().get("X-RateLimit-Remaining"));
    }

    @Test
    void testQuickCheckEndpoint() {
        ResponseEntity<RateLimitResponse> response = restTemplate.getForEntity(
                baseUrl + "/check/integration:user:2", RateLimitResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isAllowed());
    }

    @Test
    void testRateLimitExceeded() {
        String identifier = "integration:user:exceed";
        RateLimitConfig config = new RateLimitConfig(2, 1, 60);

        RateLimitRequest request = new RateLimitRequest();
        request.setIdentifier(identifier);
        request.setConfig(config);

        // First two requests should be allowed
        for (int i = 0; i < 2; i++) {
            ResponseEntity<RateLimitResponse> response = restTemplate.postForEntity(
                    baseUrl + "/check", request, RateLimitResponse.class);
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        // Third request should be denied
        ResponseEntity<RateLimitResponse> response = restTemplate.postForEntity(
                baseUrl + "/check", request, RateLimitResponse.class);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertFalse(response.getBody().isAllowed());
    }

    @Test
    void testResetRateLimit() {
        String identifier = "integration:user:reset";

        // Reset endpoint
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/reset/" + identifier,
                HttpMethod.DELETE,
                null,
                Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().containsKey("message"));
    }

    @Test
    void testMetricsEndpoint() {
        // Make some requests first
        restTemplate.getForEntity(baseUrl + "/check/metrics:user:1", RateLimitResponse.class);
        restTemplate.getForEntity(baseUrl + "/check/metrics:user:2", RateLimitResponse.class);

        ResponseEntity<RateLimitMetrics> response = restTemplate.getForEntity(
                baseUrl + "/metrics", RateLimitMetrics.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getTotalRequests() >= 2);
    }

    @Test
    void testHealthCheckEndpoint() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/health", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UP", response.getBody().get("status"));
    }
}