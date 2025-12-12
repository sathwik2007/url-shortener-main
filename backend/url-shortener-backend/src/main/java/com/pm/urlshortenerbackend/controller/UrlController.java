package com.pm.urlshortenerbackend.controller;

import com.pm.urlshortenerbackend.dto.*;
import com.pm.urlshortenerbackend.exception.UrlExpiredException;
import com.pm.urlshortenerbackend.exception.UrlNotFoundException;
import com.pm.urlshortenerbackend.health.UrlShortenerMetrics;
import com.pm.urlshortenerbackend.model.User;
import com.pm.urlshortenerbackend.security.UserPrincipal;
import com.pm.urlshortenerbackend.service.ClickTrackingService;
import com.pm.urlshortenerbackend.service.UrlService;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * Author: sathwikpillalamarri
 * Date: 9/29/25
 * Project: url-shortener-backend
 */
@RestController
@RequestMapping("/api")
public class UrlController {
    private static final Logger log = LoggerFactory.getLogger(UrlController.class);
    private final UrlService urlService;
    private final ClickTrackingService clickTrackingService;
    private final UrlShortenerMetrics metrics;

    public UrlController(UrlService urlService, ClickTrackingService clickTrackingService, UrlShortenerMetrics metrics) {
        this.urlService = urlService;
        this.clickTrackingService = clickTrackingService;
        this.metrics = metrics;
    }
    /*
    * Create short URL - supports both authenticated and anonymous users
    * Authenticated users get ownership, anonymous users can create public URLs
    * */
    @PostMapping("/links")
    public ResponseEntity<CreateUrlResponse> createShortUrl(@Valid @RequestBody CreateUrlRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Received createShortUrl request for: {}", request.getOriginalUrl());
        Timer.Sample sample = metrics.startUrlCreationTimer();

        try {
            User user = extractUser(userDetails);
            String userInfo = user != null ? user.getEmail() : "anonymous";
            log.info("Creating short URL for user: {} with URL: {}", userInfo, request.getOriginalUrl());

            CreateUrlResponse response;
            if(user != null) {
                // Authenticated user - create with ownership
                response = urlService.createShortUrl(request, user);
                log.info("Created authenticated short URL: {} for user: {}", response.getShortUrl(), user.getEmail());
            } else {
                // Anonymous user - create public URL
                response = urlService.createShortUrl(request);
                log.info("Created anonymous short URL: {}", response.getShortUrl());
            }

            metrics.incrementUrlCreation();
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating short URL: {}", e.getMessage());
            metrics.incrementError();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            metrics.recordUrlCreationTime(sample);
        }
    }

    /*
    * Get user's URLs with pagination
    * Requires authentication
    **/
    @GetMapping("/links")
    public ResponseEntity<Page<UrlMappingResponse>> getUserUrls(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            User user = extractUser(userDetails);
            if(user == null) {
                log.warn("Unauthenticated request to get user URLs");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            log.debug("Getting URLs for user: {} (page: {}, size:{}, activeOnly: {})", user.getEmail(), page, size, activeOnly);

            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<UrlMappingResponse> urls;
            if(activeOnly) {
                urls = urlService.getUserActiveUrls(user, pageable);
            } else {
                urls = urlService.getUserUrls(user, pageable);
            }

            log.debug("Retrieved {} URLs for user: {}", urls.getNumberOfElements(), user.getEmail());
            return ResponseEntity.ok(urls);
        } catch (Exception e) {
            log.error("Error retrieving user URLs: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    /*
    * Get Specific URL details
    * Requires authentication and ownership
    */
    @GetMapping("/links/{shortCode}")
    public ResponseEntity<UrlMappingResponse> getUrlDetails(
            @PathVariable
            @Pattern(regexp = "^[0-9a-zA-Z]{1,10}$", message = "Invalid short code format")
            String shortCode,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            User user = extractUser(userDetails);
            if(user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            log.debug("Getting URL details for shortCode: {} by user: {}", shortCode, user.getEmail());

            urlService.validateUrlOwnership(shortCode, user);

            UrlMappingResponse urlMapping = urlService.getUrlMapping(shortCode);
            return  ResponseEntity.ok(urlMapping);
        } catch (UrlNotFoundException e) {
            log.warn("URL not found: {}", shortCode);
            return  ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting URL details for shortCode: {}", shortCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    /*
    * Update URL
    * Requires authentication and ownership
    */
    @PutMapping("/links/{shortCode}")
    public ResponseEntity<UrlMappingResponse> updateUrl(
            @PathVariable
            @Pattern(regexp = "^[0-9a-zA-Z]{1,10}$", message = "Invalid short code format")
            String shortCode,
            @Valid @RequestBody CreateUrlRequest updateRequest,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try{
            User user = extractUser(userDetails);
            if(user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            log.info("Updating URL shortCode: {} by user: {}", shortCode, user.getEmail());

            urlService.validateUrlOwnership(shortCode, user);

            UrlMappingResponse urlMapping = urlService.updateUrl(shortCode, updateRequest, user);
            log.info("Successfully update URL shortCode: {}", shortCode);
            return ResponseEntity.ok(urlMapping);
        } catch (UrlNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid update request for shortCode: {} - {}", shortCode, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error updating URL shortCode: {}", shortCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /*
    * Delete URL(deactivate)
    * Requires authentication and ownership
    */
    @DeleteMapping("/links/{shortCode}")
    public ResponseEntity<Void> deleteUrl(
            @PathVariable
            @Pattern(regexp = "^[0-9a-zA-Z]{1,10}$", message = "Invalid Short Code Format")
            String shortCode,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            User user = extractUser(userDetails);
            if(user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            log.info("Deleting URL shortCode: {} by user: {}", shortCode, user.getEmail());

            urlService.validateUrlOwnership(shortCode, user);

            urlService.deactivateUrl(shortCode, user);
            log.info("Successfully deleted URL shortCode: {}",shortCode);
            return  ResponseEntity.noContent().build();
        } catch (UrlNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting URL shortCode: {}", shortCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /*
    * Reactivate a deactivated URL
    * Requires authentication and ownership
    */
    @PatchMapping("/links/{shortCode}/reactivate")
    public ResponseEntity<UrlMappingResponse> reactivateUrl(
            @PathVariable
            @Pattern(regexp = "^[0-9a-zA-Z]{1,10}$", message = "Invalid short code format")
            String shortCode,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            User user = extractUser(userDetails);
            if(user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            log.info("Reactivating a URL shortCode: {} by user: {}", shortCode, user.getEmail());

            urlService.reactivateUrl(shortCode, user);
            UrlMappingResponse urlMapping = urlService.getUrlMapping(shortCode);

            log.info("Successfully reactivated URL shortCode: {}", shortCode);
            return ResponseEntity.ok(urlMapping);
        } catch (UrlNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (UrlExpiredException e) {
            log.warn("Cannot reactivate expired URL: {}", shortCode);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Error reactivating URL shortCode: {}", shortCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /*
    * Get user's URL statistics summary
    * Requires authentication
    */
    @GetMapping("/links/summary")
    public ResponseEntity<UserUrlSummary> getUserUrlSummary(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            User user = extractUser(userDetails);
            if(user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            log.debug("Getting URL summary for user: {}", user.getEmail());

            long totalUrls = urlService.getUserUrlCount(user);
            long activeUrls = urlService.getUserActiveUrlCount(user);
            UserUrlSummary summary = new UserUrlSummary(totalUrls, activeUrls);

            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error getting URL summary: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /*
    * Redirect endpoint - handles both authenticated and anonymous URLs
    */
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirectToOriginalUrl(
            @PathVariable
            @Pattern(regexp = "^[0-9a-zA-Z]{1,10}$", message = "Invalid Short Code Format")
            String shortCode,
            HttpServletRequest request
    ) {
        Timer.Sample sample = metrics.startRedirectTimer();
        try {
            log.info("Redirect request for shortCode: {}", shortCode);

            String originalUrl = urlService.getOriginalUrl(shortCode);

            try {
                ClickEventData clickEventData = extractClickEventData(request);
                clickTrackingService.logClick(shortCode, clickEventData);
            } catch (Exception e) {
                log.error("Failed to initiate click tracking for shortCode: {}, continuing with redirect", shortCode);
            }

            log.info("Redirected {} -> {}", shortCode, originalUrl);

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(originalUrl))
                    .build();
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

    // Extract User from UserDetails
    private User extractUser(UserDetails userDetails) {
        if(userDetails instanceof UserPrincipal) {
            return ((UserPrincipal) userDetails).getUser();
        }
        return null;
    }

    // Extract click event data from request
    private ClickEventData extractClickEventData(HttpServletRequest request) {
        String ipAddress = extractIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String referrer = request.getHeader("Referer");
        return new ClickEventData(ipAddress, userAgent, referrer);
    }

    // Extracts IP address from request
    private String extractIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if(ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        if(ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        if(ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return ipAddress != null ? ipAddress : "unknown";
    }
}
