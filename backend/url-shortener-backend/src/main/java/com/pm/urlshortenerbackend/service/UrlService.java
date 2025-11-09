package com.pm.urlshortenerbackend.service;

import com.pm.urlshortenerbackend.dto.CreateUrlRequest;
import com.pm.urlshortenerbackend.dto.CreateUrlResponse;
import com.pm.urlshortenerbackend.dto.UrlMappingResponse;
import com.pm.urlshortenerbackend.exception.UnauthorizedAccessException;
import com.pm.urlshortenerbackend.exception.UrlExpiredException;
import com.pm.urlshortenerbackend.model.UrlMapping;
import com.pm.urlshortenerbackend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Author: sathwikpillalamarri
 * Date: 9/28/25
 * Project: url-shortener-backend
 */
public interface UrlService {
    CreateUrlResponse createShortUrl(CreateUrlRequest request);

    String getOriginalUrl(String shortCode);

    UrlMappingResponse getUrlMapping(String shortCode);

    //Methods for URL Ownership
    CreateUrlResponse createShortUrl(CreateUrlRequest request, User owner);

    Page<UrlMappingResponse> getUserUrls(User owner, Pageable pageable);

    Page<UrlMappingResponse> getUserActiveUrls(User owner, Pageable pageable);

    boolean isUrlOwnedByUser(String shortCode, User user);

    void validateUrlOwnership(String shortCode, User user) throws UnauthorizedAccessException;

    long getUserUrlCount(User owner);

    long getUserActiveUrlCount(User owner);

    //Methods for handling link expiration
    boolean isUrlExpired(String shortCode);

    void validateUrlNotExpired(String shortCode) throws UrlExpiredException;

    List<UrlMapping> findExpiredUrls();

    int deactivateExpiredUrls();
}
