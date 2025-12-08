package com.pm.urlshortenerbackend.controller;

import com.pm.urlshortenerbackend.dto.ClickEventData;
import com.pm.urlshortenerbackend.exception.UrlExpiredException;
import com.pm.urlshortenerbackend.exception.UrlNotFoundException;
import com.pm.urlshortenerbackend.health.UrlShortenerMetrics;
import com.pm.urlshortenerbackend.service.ClickTrackingService;
import com.pm.urlshortenerbackend.service.UrlService;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Author: Sathwik Pillalamarri
 * Date: 12/2/25
 * Project: url-shortener-backend
 */
@RestController
public class RedirectController {
    private static final Logger log = LoggerFactory.getLogger(RedirectController.class);
    private final UrlService urlService;
    private final ClickTrackingService clickTrackingService;
    private final UrlShortenerMetrics metrics;

    public RedirectController(UrlService urlService, ClickTrackingService clickTrackingService, UrlShortenerMetrics urlShortenerMetrics) {
        this.urlService = urlService;
        this.clickTrackingService = clickTrackingService;
        this.metrics = urlShortenerMetrics;
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable
            @Pattern(regexp = "^[0-9a-zA-Z]{1,10}$", message = "Invalid short code format")
            String shortCode,
            HttpServletRequest request
    ) {
        Timer.Sample sample = metrics.startRedirectTimer();
        try {
            log.debug("Public redirect request for shortCode: {}", shortCode);

            String originalUrl = urlService.getOriginalUrl(shortCode);

            ClickEventData clickEventData = extractClickEventData(request);
            clickTrackingService.logClick(shortCode, clickEventData);

            log.debug("Redirecting {} to {}", shortCode, originalUrl);

            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(originalUrl)).build();
        } catch (UrlNotFoundException e) {
            log.warn("Short code not found: {}", shortCode);
            return ResponseEntity.notFound().build();
        } catch (UrlExpiredException e) {
            log.warn("Short code expired: {}", shortCode);
            return ResponseEntity.status(HttpStatus.GONE).build();
        } catch (Exception e) {
            log.error("Error redirecting for short code: {}", shortCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            metrics.recordRedirectTime(sample);
        }
    }

    private ClickEventData extractClickEventData(HttpServletRequest request) {
        String ipAddress = extractIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String referrer = request.getHeader("Referer");
        return new ClickEventData(ipAddress, userAgent, referrer);
    }

    private String extractIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return ipAddress != null ? ipAddress : "unknown";
    }
}
