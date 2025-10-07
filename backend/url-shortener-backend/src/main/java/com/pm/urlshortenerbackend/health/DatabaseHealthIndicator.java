package com.pm.urlshortenerbackend.health;

import com.pm.urlshortenerbackend.repository.UrlMappingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Author: Sathwik Pillalamarri
 * Date: 10/7/25
 * Project: url-shortener-backend
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    private final UrlMappingRepository urlMappingRepository;

    @Value("${app.health.database.timeout:10000}")
    private long timeout;

    public DatabaseHealthIndicator(UrlMappingRepository urlMappingRepository) {
        this.urlMappingRepository = urlMappingRepository;
    }

    @Override
    public Health health() {
        try {
            long count = urlMappingRepository.count();

            return Health.up()
                    .withDetail("database", "Available")
                    .withDetail("totalUrls", count)
                    .withDetail("status", "Connected")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("database", "Connection Failed")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
