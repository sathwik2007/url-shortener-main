package com.pm.urlshortenerbackend.service.impl;

import com.pm.urlshortenerbackend.dto.CreateUrlRequest;
import com.pm.urlshortenerbackend.dto.CreateUrlResponse;
import com.pm.urlshortenerbackend.dto.UrlMappingResponse;
import com.pm.urlshortenerbackend.exception.InvalidUrlException;
import com.pm.urlshortenerbackend.exception.UrlNotFoundException;
import com.pm.urlshortenerbackend.model.UrlMapping;
import com.pm.urlshortenerbackend.repository.UrlMappingRepository;
import com.pm.urlshortenerbackend.service.CacheService;
import com.pm.urlshortenerbackend.service.UrlService;
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

    private final UrlMappingRepository repository;
    private final IdGenerationServiceImpl idGenerationServiceImpl;
    private final CacheService cacheService;

    private final String baseUrl;

    private final int maxLength;

    private final long cacheTtl;

    private final boolean enableDuplicateDetection;

    public UrlServiceImpl(UrlMappingRepository repository,
                          IdGenerationServiceImpl idGenerationServiceImpl,
                          CacheService cacheService,
                          @Value("${app.base-url}") String baseUrl,
                          @Value("${app.url.max-length:2048}") int maxLength,
                          @Value("${app.url.cache-ttl:3600}") long cacheTtl,
                          @Value("${app.url.enable-duplicate-detection:true}") boolean enableDuplicateDetection) {
        this.repository = repository;
        this.idGenerationServiceImpl = idGenerationServiceImpl;
        this.cacheService = cacheService;
        this.baseUrl = baseUrl;
        this.maxLength = maxLength;
        this.cacheTtl = cacheTtl;
        this.enableDuplicateDetection = enableDuplicateDetection;
    }

    @Override
    public CreateUrlResponse createShortUrl(CreateUrlRequest request) {
        String originalUrl = validateUrl(request.getOriginalUrl());
        if(enableDuplicateDetection) {
            Optional<UrlMapping> existing = repository.findByOriginalUrl(originalUrl);
            if(existing.isPresent()) {
                UrlMapping mapping = existing.get();
                cacheService.putUrlMapping(mapping.getShortCode(), mapping, cacheTtl);
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
        return buildResponse(mapping);
    }


    @Override
    public String getOriginalUrl(String shortCode) {
        return getUrlMapping(shortCode).getOriginalUrl();
    }

    @Override
    public UrlMappingResponse getUrlMapping(String shortCode) {
        Optional<UrlMapping> cached = cacheService.getUrlMapping(shortCode, UrlMapping.class);
        if(cached.isPresent()) {
            return buildMappingResponse(cached.get());
        }

        UrlMapping mapping = repository.findByShortCode(shortCode).orElseThrow(() -> new UrlNotFoundException(shortCode));

        cacheService.putUrlMapping(shortCode, mapping, cacheTtl);
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
            baseUrl + "/" + mapping.getShortCode(),
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
