package com.pm.urlshortenerbackend.service;

import com.pm.urlshortenerbackend.dto.CreateUrlRequest;
import com.pm.urlshortenerbackend.dto.CreateUrlResponse;
import com.pm.urlshortenerbackend.dto.UrlMappingResponse;

/**
 * Author: sathwikpillalamarri
 * Date: 9/28/25
 * Project: url-shortener-backend
 */
public interface UrlService {
    CreateUrlResponse createShortUrl(CreateUrlRequest request);

    String getOriginalUrl(String shortCode);

    UrlMappingResponse getUrlMapping(String shortCode);
}
