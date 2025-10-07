package com.pm.urlshortenerbackend.health;

import com.pm.urlshortenerbackend.repository.UrlMappingRepository;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.util.Map;

/**
 * Author: Sathwik Pillalamarri
 * Date: 10/7/25
 * Project: url-shortener-backend
 */
@Component
public class UrlShortenerInfoContributor implements InfoContributor {
    private final UrlMappingRepository urlMappingRepository;

    public UrlShortenerInfoContributor(UrlMappingRepository urlMappingRepository) {
        this.urlMappingRepository = urlMappingRepository;
    }

    @Override
    public void contribute(Info.Builder builder) {
        // Always add application details
        builder.withDetail("application", Map.of(
                "name", "URL Shortener",
                "version", "1.0.0",
                "description", "A simple URL shortening service"
        ));

        try {
            long totalUrls = urlMappingRepository.count();

            builder.withDetail("statistics", Map.of(
                    "totalUrls", totalUrls,
                    "uptime", ManagementFactory.getRuntimeMXBean().getUptime()
            ));
        } catch (Exception e) {
            builder.withDetail("statistics", Map.of("error", "Unable to fetch statistics: " + e.getMessage()));
        }
    }
}
