package com.pm.urlshortenerbackend.scheduler;

import com.pm.urlshortenerbackend.service.UrlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Author: Sathwik Pillalamarri
 * Date: 11/9/25
 * Project: url-shortener-backend
 */
@Component
public class UrlExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(UrlExpirationScheduler.class);
    private final UrlService urlService;

    public UrlExpirationScheduler(UrlService urlService) {
        this.urlService = urlService;
    }

    @Scheduled(fixedRate = 3600000)
    public void deactivateExpiredUrls() {
        log.info("Starting scheduled cleanup of expired URLs");

        try {
            int deactivatedCount = urlService.deactivateExpiredUrls();
            log.info("Scheduled cleanup completed. Deactivated {} URLs", deactivatedCount);
        } catch (Exception e) {
            log.error("Error during scheduled URL cleanup");
        }
    }
}
