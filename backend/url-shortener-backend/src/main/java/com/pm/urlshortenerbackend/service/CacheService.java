package com.pm.urlshortenerbackend.service;

import java.util.Optional;

/**
 * Author: sathwikpillalamarri
 * Date: 9/26/25
 * Project: url-shortener-backend
 */
public interface CacheService {
    <T> Optional<T> get(String key, Class<T> type);

    void put(String key, Object value, long ttlSeconds);

    boolean delete(String key);

    boolean exists(String key);

    <T> Optional<T> getUrlMapping(String shortCode, Class<T> type);

    void putUrlMapping(String shortCode, Object urlMapping, long ttlSeconds);

    boolean deleteUrlMapping(String shortCode);

    boolean existsUrlMapping(String shortCode);

    void evictPattern(String pattern);
}
