package com.pm.urlshortenerbackend.service;

/**
 * Author: sathwikpillalamarri
 * Date: 9/29/25
 * Project: url-shortener-backend
 */
public interface IdGenerationService {
    String generateUniqueId(long value);

    String generateUniqueShortCode();
}
