package com.pm.urlshortenerbackend.service.impl;

import com.pm.urlshortenerbackend.dto.CreateUrlRequest;
import com.pm.urlshortenerbackend.dto.CreateUrlResponse;
import com.pm.urlshortenerbackend.dto.UrlMappingResponse;
import com.pm.urlshortenerbackend.exception.InvalidUrlException;
import com.pm.urlshortenerbackend.exception.UnauthorizedAccessException;
import com.pm.urlshortenerbackend.exception.UrlExpiredException;
import com.pm.urlshortenerbackend.exception.UrlNotFoundException;
import com.pm.urlshortenerbackend.health.UrlShortenerMetrics;
import com.pm.urlshortenerbackend.model.UrlMapping;
import com.pm.urlshortenerbackend.model.User;
import com.pm.urlshortenerbackend.repository.UrlMappingRepository;
import com.pm.urlshortenerbackend.service.CacheService;
import com.pm.urlshortenerbackend.service.UrlService;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
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

    //Kept this method to ensure backward compatibility for anonymous URLs
    @Override
    public CreateUrlResponse createShortUrl(CreateUrlRequest request) {
        return createShortUrl(request, null);
    }


    @Override
    public String getOriginalUrl(String shortCode) {
        logger.debug("Retrieving original URL for short code: {}", shortCode);
        Timer.Sample sample = metrics.startUrlRetrievalTimer();

        try {
            UrlMappingResponse mappingResponse = getUrlMapping(shortCode);

            //Check if the original URL is expired
            if(isUrlExpired(shortCode)) {
                logger.warn("Attempted access to expired URL: {}", shortCode);
                metrics.incrementError();
                throw new UrlExpiredException(shortCode);
            }

            String originalUrl = mappingResponse.getOriginalUrl();
            logger.debug("Successfully retrieved original URL for short code: {}", shortCode);
            return originalUrl;
        } catch (UrlNotFoundException e) {
            logger.warn("Short code not found: {}", shortCode);
            metrics.incrementError();
            throw e;
        } catch (UrlExpiredException e) {
            logger.warn("Short code expired: {}", shortCode);
            metrics.incrementError();
            throw e;
        }
        catch (Exception e) {
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

    @Override
    public CreateUrlResponse createShortUrl(CreateUrlRequest request, User owner) {
        logger.info("Creating short URL for user {} with URL: {}", owner != null ? owner.getEmail() : "anonymous", request.getOriginalUrl());
        Timer.Sample sample = metrics.startUrlCreationTimer();

        try {
            String originalUrl = validateUrl(request.getOriginalUrl());

            //Validate expiration date if it exists
            if(request.getExpiresAt() != null) {
                validateExpirationDate(request.getExpiresAt());
            }

            if(enableDuplicateDetection) {
                if(owner != null) {
                    // For authenticated users, check if there is already an existing original URL and retrieve the short if exists
                    Optional<UrlMapping> existing = repository.findByOriginalUrlAndOwner(originalUrl, owner);
                    if (existing.isPresent()) {
                        UrlMapping mapping = existing.get();
                        cacheService.putUrlMapping(mapping.getShortCode(), mapping, cacheTtl);
                        logger.info("Returned existing short URL : {} for user {}", mapping.getShortCode(), owner.getEmail());
                        metrics.incrementUrlCreation();
                        return buildResponse(mapping);
                    }
                } else {
                    // For anonymous users, check global duplicates
                    Optional<UrlMapping> existing = repository.findByOriginalUrl(originalUrl);
                    if (existing.isPresent()) {
                        UrlMapping mapping = existing.get();
                        cacheService.putUrlMapping(mapping.getShortCode(), mapping, cacheTtl);
                        logger.info("Returned existing short URL : {} for anonymous user", mapping.getShortCode());
                        metrics.incrementUrlCreation();
                        return buildResponse(mapping);
                    }
                }
            }

            String shortCode = idGenerationServiceImpl.generateUniqueShortCode();

            UrlMapping mapping = new UrlMapping();
            mapping.setOriginalUrl(originalUrl);
            mapping.setCreatedAt(LocalDateTime.now());
            mapping.setShortCode(shortCode);
            mapping.setOwner(owner);

            if(request.getExpiresAt() != null) {
                mapping.setExpiresAt(request.getExpiresAt());
            }

            repository.save(mapping);

            //Updating the user's URL list if the owner exists
            if(owner != null) {
                owner.addUrlMapping(mapping);
            }

            cacheService.putUrlMapping(shortCode, mapping, cacheTtl);
            logger.info("Successfully created new short URL : {} for user: {}", mapping.getShortCode(), owner != null ? owner.getEmail() : "anonymous");
            metrics.incrementUrlCreation();
            return buildResponse(mapping);
        } catch (Exception e) {
            logger.error("Failed to create short URL for user: {}, error: {}", owner != null ? owner.getEmail() : "anonymous", e.getMessage());
            metrics.incrementError();
            throw e;
        } finally {
            metrics.recordUrlCreationTime(sample);
        }
    }

    private void validateExpirationDate(LocalDateTime expiresAt) {
        if(expiresAt.isBefore(LocalDateTime.now())) {
            throw new InvalidUrlException("Expiration date cannot be in the past");
        }

        //Condition for setting max expiration
        LocalDateTime maxExpiration = LocalDateTime.now().plusYears(1);
        if(expiresAt.isAfter(maxExpiration)) {
            throw new InvalidUrlException("Expiration date cannot be more than 1 year from now");
        }
    }

    @Override
    public Page<UrlMappingResponse> getUserUrls(User owner, Pageable pageable) {
        if(owner == null) {
            throw new IllegalArgumentException("Owner cannot be null");
        }
        
        logger.debug("Retrieving URLs for user: {} with pagination: {}", owner.getEmail(), pageable);

        try {
            Page<UrlMapping> urlMappings = repository.findByOwner(owner, pageable);
            Page<UrlMappingResponse> responses = urlMappings.map(this::buildMappingResponse);

            logger.debug("Retrieved {} URLs for user: {}", responses.getNumberOfElements(), owner.getEmail());
            return responses;
        } catch (Exception e) {
            logger.error("Failed to retrieve URLs for user: {}, error: {}", owner.getEmail(), e.getMessage());
            metrics.incrementError();
            throw e;
        }
    }

    @Override
    public Page<UrlMappingResponse> getUserActiveUrls(User owner, Pageable pageable) {
        if(owner == null) {
            throw new IllegalArgumentException("Owner cannot be null");
        }
        
        logger.debug("Retrieving active URLs for users: {} with pagination: {}", owner.getEmail(), pageable);

        try {
            Page<UrlMapping> urlMappings = repository.findByOwnerAndIsActive(owner, true, pageable);
            Page<UrlMappingResponse> responses = urlMappings.map(this::buildMappingResponse);

            logger.debug("Retrieved {} active URLs for user: {}", responses.getNumberOfElements(), owner.getEmail());
            return responses;
        } catch (Exception e) {
            logger.error("Failed to retrieve active URLs for user: {}, error: {}", owner.getEmail(), e.getMessage());
            metrics.incrementError();
            throw e;
        }
    }

    @Override
    public boolean isUrlOwnedByUser(String shortCode, User user) {
        if(user == null || shortCode == null) {
            return false;
        }
        try {
            Optional<UrlMapping> mapping = repository.findByShortCodeAndOwner(shortCode, user);
            return mapping.isPresent();
        } catch (Exception e) {
            logger.error("Error checking URL ownership for shortCode: {} and user: {}", shortCode, user.getEmail(), e);
            return false;
        }
    }

    @Override
    public void validateUrlOwnership(String shortCode, User user) throws UnauthorizedAccessException {
        logger.debug("Validating URL ownership for shortCode: {} and user: {}", shortCode, user != null ? user.getEmail() : "null");

        if(user == null) {
            throw new UnauthorizedAccessException("User Authentication Required");
        }

        if(!isUrlOwnedByUser(shortCode, user)) {
            logger.warn("Unauthorized access attempt: User {} tried to access shortCode: {}", user.getEmail(), shortCode);
            throw new UnauthorizedAccessException("You don't have permission to access this URL");
        }

        logger.debug("URL ownership validated successfully for shortCode: {} and user: {}", shortCode, user.getEmail());
    }

    @Override
    public long getUserUrlCount(User owner) {
        if(owner == null) {
            throw new IllegalArgumentException("Owner cannot be null");
        }

        try {
            long count = repository.countByOwner(owner);
            logger.debug("User {} has {} total URLs", owner.getEmail(), count);
            return count;
        } catch (Exception e) {
            logger.error("Failed to count URLs for user: {}, error: {}", owner.getEmail(), e.getMessage());
            metrics.incrementError();
            throw e;
        }
    }

    @Override
    public long getUserActiveUrlCount(User owner) {
        if(owner == null) {
            throw new IllegalArgumentException("Owner cannot be null");
        }

        try {
            long count = repository.countByOwnerAndIsActive(owner, true);
            logger.debug("User {} has {} active URLs", owner.getEmail(), count);
            return count;
        } catch (Exception e) {
            logger.error("Failed to count active URLs for user: {}, error: {}", owner.getEmail(), e.getMessage());
            metrics.incrementError();
            throw e;
        }
    }

    @Override
    public boolean isUrlExpired(String shortCode) {
        try {
            //Check the cache first for the original URL
            Optional<UrlMapping> cached = cacheService.getUrlMapping(shortCode, UrlMapping.class);
            if(cached.isPresent()) {
                return cached.get().isExpired();
            }

            //If cache is a miss, fetch from the database
            Optional<UrlMapping> mapping = repository.findByShortCode(shortCode);
            return mapping.map(UrlMapping::isExpired).orElse(false);
        } catch (Exception e) {
            logger.error("Error checking expiration for shortCode: {}", shortCode, e);
            return false;
        }
    }

    @Override
    public void validateUrlNotExpired(String shortCode) throws UrlExpiredException {
        if(isUrlExpired(shortCode)) {
            throw new UrlExpiredException(shortCode);
        }
    }

    @Override
    public List<UrlMapping> findExpiredUrls() {
        try {
            List<UrlMapping> expiredUrls = repository.findExpiredUrls(LocalDateTime.now());
            logger.debug("Found {} expired URLs", expiredUrls.size());
            return expiredUrls;
        } catch (Exception e) {
            logger.error("Error finding expired URLs: {}", e.getMessage());
            metrics.incrementError();
            throw e;
        }
    }

    @Override
    public int deactivateExpiredUrls() {
        try {
            int deactivatedCount = repository.deactivateExpiredUrls(LocalDateTime.now());
            logger.info("Deactivated {} expired URLs", deactivatedCount);
            metrics.incrementUrlDeactivation(deactivatedCount);
            return  deactivatedCount;
        } catch (Exception e) {
            logger.error("Error deactivating expired URLs: {}", e.getMessage());
            metrics.incrementError();
            throw e;
        }
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
                mapping.getCreatedAt(),
                mapping.getExpiresAt(),
                mapping.getIsActive());
    }
}
