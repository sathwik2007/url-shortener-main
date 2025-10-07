package com.pm.urlshortenerbackend.service.impl;

import com.pm.urlshortenerbackend.dto.CreateUrlRequest;
import com.pm.urlshortenerbackend.dto.CreateUrlResponse;
import com.pm.urlshortenerbackend.dto.UrlMappingResponse;
import com.pm.urlshortenerbackend.exception.InvalidUrlException;
import com.pm.urlshortenerbackend.exception.UrlNotFoundException;
import com.pm.urlshortenerbackend.health.UrlShortenerMetrics;
import com.pm.urlshortenerbackend.model.UrlMapping;
import com.pm.urlshortenerbackend.repository.UrlMappingRepository;
import com.pm.urlshortenerbackend.service.CacheService;
import com.pm.urlshortenerbackend.service.UrlService;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Author: sathwikpillalamarri
 * Date: 9/28/25
 * Project: url-shortener-backend
 */
@Service
@Transactional
public class UrlServiceImpl implements UrlService {

    private static final Logger logger = LoggerFactory.getLogger(UrlServiceImpl.class);
    private final UrlMappingRepository repository;
    private final IdGenerationServiceImpl idGenerationServiceImpl;
    private final CacheService cacheService;
    private final UrlShortenerMetrics metrics;

    private final String baseUrl;

    private final int maxLength;

    private final long cacheTtl;

    private final boolean enableDuplicateDetection;

    public UrlServiceImpl(UrlMappingRepository repository,
                          IdGenerationServiceImpl idGenerationServiceImpl,
                          CacheService cacheService,
                          UrlShortenerMetrics metrics,
                          @Value("${app.base-url}") String baseUrl,
                          @Value("${app.url.max-length:2048}") int maxLength,
                          @Value("${app.url.cache-ttl:3600}") long cacheTtl,
                          @Value("${app.url.enable-duplicate-detection:true}") boolean enableDuplicateDetection) {
        this.repository = repository;
        this.idGenerationServiceImpl = idGenerationServiceImpl;
        this.cacheService = cacheService;
        this.metrics = metrics;
        this.baseUrl = baseUrl;
        this.maxLength = maxLength;
        this.cacheTtl = cacheTtl;
        this.enableDuplicateDetection = enableDuplicateDetection;
    }

    @Override
    public CreateUrlResponse createShortUrl(CreateUrlRequest request) {
        logger.info("Creating short URL for: {}", request.getOriginalUrl());
        Timer.Sample sample = metrics.startUrlCreationTimer();

        try {
            String originalUrl = validateUrl(request.getOriginalUrl());
            if(enableDuplicateDetection) {
                Optional<UrlMapping> existing = repository.findByOriginalUrl(originalUrl);
                if(existing.isPresent()) {
                    UrlMapping mapping = existing.get();
                    cacheService.putUrlMapping(mapping.getShortCode(), mapping, cacheTtl);
                    logger.info("Successfully returned existing short URL: {} for original URL: {}", mapping.getShortCode(), request.getOriginalUrl());
                    metrics.incrementUrlCreation();
                    return buildResponse(mapping);
                }
            }

            String shortCode = idGenerationServiceImpl.generateUniqueShortCode();

            UrlMapping mapping = new UrlMapping();
            mapping.setOriginalUrl(originalUrl);
            mapping.setCreatedAt(LocalDateTime.now());
            mapping.setShortCode(shortCode);

            repository.save(mapping);

            cacheService.putUrlMapping(shortCode, mapping, cacheTtl);
            logger.info("Successfully created new short URL: {} for original URL: {}", mapping.getShortCode(), request.getOriginalUrl());
            metrics.incrementUrlCreation();
            return buildResponse(mapping);
        } catch (Exception e) {
            logger.error("Failed to create short URL for: {}, error: {}", request.getOriginalUrl(), e.getMessage());
            metrics.incrementError();
            throw e;
        } finally {
            metrics.recordUrlCreationTime(sample);
        }
    }


    @Override
    public String getOriginalUrl(String shortCode) {
        logger.debug("Retrieving original URL for short code: {}", shortCode);
        Timer.Sample sample = metrics.startUrlRetrievalTimer();

        try {
            String originalUrl = getUrlMapping(shortCode).getOriginalUrl();
            logger.debug("Successfully retrieved original URL for short code: {}", shortCode);
            return originalUrl;
        } catch (UrlNotFoundException e) {
            logger.warn("Short code not found: {}", shortCode);
            metrics.incrementError();
            throw e;
        } catch (Exception e) {
            logger.error("Failed to retrieve URL for short code: {}, error: {}", shortCode, e.getMessage());
            metrics.incrementError();
            throw e;
        } finally {
            metrics.recordUrlRetrievalTime(sample);
        }
    }

    @Override
    public UrlMappingResponse getUrlMapping(String shortCode) {
        // Check cache first
        Optional<UrlMapping> cached = cacheService.getUrlMapping(shortCode, UrlMapping.class);
        if(cached.isPresent()) {
            logger.debug("Cache hit for short code: {}", shortCode);
            metrics.incrementCacheHit();
            metrics.incrementUrlRetrieval();
            return buildMappingResponse(cached.get());
        }

        logger.debug("Cache miss for short code: {}, querying database", shortCode);
        metrics.incrementCacheMiss();

        UrlMapping mapping = repository.findByShortCode(shortCode)
            .orElseThrow(() -> new UrlNotFoundException(shortCode));

        // Cache the result for future requests
        cacheService.putUrlMapping(shortCode, mapping, cacheTtl);
        metrics.incrementUrlRetrieval();
        return buildMappingResponse(mapping);
    }

    private String validateUrl(String url) {
        if(url == null || url.length() > maxLength) {
            throw new InvalidUrlException("URL is null or exceeds max length");
        }
        try {
            URL parsed = new URL(url);
            if(!parsed.getProtocol().matches("https?")) {
                throw new InvalidUrlException("Invalid Protocol: " + parsed.getProtocol());
            }
        } catch (MalformedURLException e) {
            throw new InvalidUrlException("Malformed URL: " + url);
        }
        return url;
    }

    private CreateUrlResponse buildResponse(UrlMapping mapping) {
        return new CreateUrlResponse(
            mapping.getShortCode(),
            baseUrl + "/api/" + mapping.getShortCode(),
            mapping.getOriginalUrl(),
            mapping.getCreatedAt()
        );
    }

    private UrlMappingResponse buildMappingResponse(UrlMapping mapping) {
        return new UrlMappingResponse(mapping.getShortCode(),
                baseUrl + "/" + mapping.getShortCode(),
                mapping.getOriginalUrl(),
                mapping.getCreatedAt());
    }
}
