package com.pm.urlshortenerbackend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author: Sathwik Pillalamarri
 * Date: 1/14/26
 * Project: url-shortener-backend
 */
@Configuration
public class RateLimitingConfig {
    // Stores buckets per IP Address
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    // Creates a rate limit bucket for authentication endpoints
    // Allows 5 requests per minute per IP
    public Bucket resolveBucket(String key) {
        return cache.computeIfAbsent(key, k -> createNewBucket());
    }

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.builder().capacity(5).refillIntervally(5, Duration.ofMinutes(1)).build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
