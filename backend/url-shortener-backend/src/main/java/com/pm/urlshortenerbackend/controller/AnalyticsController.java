package com.pm.urlshortenerbackend.controller;

import com.pm.urlshortenerbackend.dto.ClickStatsResponse;
import com.pm.urlshortenerbackend.dto.DailyClickStats;
import com.pm.urlshortenerbackend.exception.UnauthorizedAccessException;
import com.pm.urlshortenerbackend.exception.UrlNotFoundException;
import com.pm.urlshortenerbackend.health.UrlShortenerMetrics;
import com.pm.urlshortenerbackend.model.User;
import com.pm.urlshortenerbackend.security.UserPrincipal;
import com.pm.urlshortenerbackend.service.AnalyticsService;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Author: Sathwik Pillalamarri
 * Date: 12/9/25
 * Project: url-shortener-backend
 */
@RestController
@RequestMapping("/api/links")
public class AnalyticsController {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);
    private final AnalyticsService analyticsService;
    private final UrlShortenerMetrics metrics;


    public AnalyticsController(AnalyticsService analyticsService, UrlShortenerMetrics metrics) {
        this.analyticsService = analyticsService;
        this.metrics = metrics;
    }

    @GetMapping("/{shortCode}/stats")
    public ResponseEntity<ClickStatsResponse> getUrlStats(
            @PathVariable
            @Pattern(regexp = "^[0-9a-zA-Z]{1,10}$", message = "Invalid short code format")
            String shortCode,
            @AuthenticationPrincipal UserDetails userDetails
            ) {
        Timer.Sample sample = metrics.startAnalyticsCalculationTimer();

        try {
            log.info("Analytics request for shortCode: {} by user: {}", shortCode, userDetails.getUsername());

            // Get user from UserDetails
            User user = getUserFromUserDetails(userDetails);

            // Get statistics with ownership validation
            ClickStatsResponse stats = analyticsService.getClickStats(shortCode, user);

            metrics.incrementAnalyticsRequest();
            log.info("Successfully retrieved analytics for shortCode: {}", shortCode);

            return ResponseEntity.ok(stats);
        } catch (UrlNotFoundException e) {
            log.warn("URL not found for analytics request: {}", shortCode);
            return ResponseEntity.notFound().build();
        } catch (UnauthorizedAccessException e) {
            log.warn("Unauthorized analytics access attempt for shortCode: {} by user: {}",shortCode, userDetails.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Error retrieving analytics for shortCode: {}", shortCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            metrics.recordAnalyticsCalculationTime(sample);
        }
    }

    @GetMapping("/{shortCode}/stats/daily")
    public ResponseEntity<List<DailyClickStats>> getDailyStats(
            @PathVariable
            @Pattern(regexp = "^[0-9a-zA-Z]{1,10}$", message = "Invalid short code format")
            String shortCode,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            log.debug("Daily stats request for shortCode:{} by user: {}", shortCode, userDetails.getUsername());

            User user = getUserFromUserDetails(userDetails);

            // Validate user ownership
            analyticsService.getClickStats(shortCode, user);

            List<DailyClickStats> dailyStats;
            if(startDate != null && endDate != null) {
                dailyStats = analyticsService.getDailyStats(shortCode, startDate, endDate);
            } else {
                dailyStats = analyticsService.getDailyStats(shortCode, days);
            }

            return ResponseEntity.ok(dailyStats);
        } catch (UrlNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (UnauthorizedAccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Error retrieving daily stats for shortCode: {}", shortCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{shortCode}/stats/total")
    public ResponseEntity<Long> getTotalClicks(
            @PathVariable
            @Pattern(regexp = "^[0-9a-zA-Z]{1,10}$", message = "Invalid short code format")
            String shortCode,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            log.debug("Total clicks request for shortCode: {} by user: {}", shortCode, userDetails.getUsername());

            User user = getUserFromUserDetails(userDetails);

            analyticsService.getClickStats(shortCode, user);

            long totalClicks = analyticsService.getTotalClicks(shortCode);
            return ResponseEntity.ok(totalClicks);
        } catch (UrlNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (UnauthorizedAccessException e) {
            return  ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Error retrieving total clicks for shortCode: {}", shortCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{shortCode}/stats/refresh")
    public ResponseEntity<Void> refreshStats(
            @PathVariable
            @Pattern(regexp = "^[0-9a-zA-Z]{1,10}$", message = "Invalid short code format")
            String shortCode,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            log.info("Stats refresh request for shortCode: {} by user: {}", shortCode, userDetails.getUsername());

            User user = getUserFromUserDetails(userDetails);

            analyticsService.getClickStats(shortCode, user);

            analyticsService.refreshCachedStats(shortCode);

            log.info("Successfully refreshed stats cache for shortCode: {}", shortCode);

            return ResponseEntity.ok().build();
        } catch (UrlNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (UnauthorizedAccessException e) {
            return  ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Error retrieving total clicks for shortCode: {}", shortCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private User getUserFromUserDetails(UserDetails userDetails) {
        if(userDetails instanceof UserPrincipal) {
            return ((UserPrincipal) userDetails).getUser();
        }

        throw new RuntimeException("Unable to extract User from UserDetails");
    }
}
