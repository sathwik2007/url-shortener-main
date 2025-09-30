package com.pm.urlshortenerbackend.controller;

import com.pm.urlshortenerbackend.dto.CreateUrlRequest;
import com.pm.urlshortenerbackend.dto.CreateUrlResponse;
import com.pm.urlshortenerbackend.service.UrlService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping("/links")
    public ResponseEntity<CreateUrlResponse> createShortUrl(@Valid @RequestBody CreateUrlRequest request) {
        log.info("Received createShortUrl request for: {}", request.getOriginalUrl());

        long start = System.currentTimeMillis();

        CreateUrlResponse response = urlService.createShortUrl(request);

        long elapsed = System.currentTimeMillis() - start;
        log.info("Generated short URL: {} in {} ms", response.getShortUrl(), elapsed);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirectToOriginalUrl(
            @PathVariable
            @Pattern(regexp = "^[0-9a-zA-Z]{1,10}$", message = "Invalid Short Code Format")
            String shortCode
    ) {
        log.info("Redirect request for shortCode: {}", shortCode);
        long start = System.currentTimeMillis();

        String originalUrl = urlService.getOriginalUrl(shortCode);

        long elapsed = System.currentTimeMillis() - start;
        log.info("Redirected {} -> {} in {} ms", shortCode, originalUrl, elapsed);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, originalUrl);

        return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
    }
}
