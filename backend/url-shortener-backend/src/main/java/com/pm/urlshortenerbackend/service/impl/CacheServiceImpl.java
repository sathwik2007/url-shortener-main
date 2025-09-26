package com.pm.urlshortenerbackend.service.impl;

import com.pm.urlshortenerbackend.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Author: Sathwik Pillalamarri
 * Date: 9/26/25
 * Project: url-shortener-backend
 */
@Service
public class CacheServiceImpl implements CacheService {
    private static final Logger log = LoggerFactory.getLogger(CacheServiceImpl.class);
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String URL_PREFIX = "url:";

    public CacheServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        try{
            ValueOperations<String, Object> ops = redisTemplate.opsForValue();
            Object value = ops.get(key);
            if(value == null) {
                log.debug("Cache miss for key: {}", key);
                return Optional.empty();
            }
            if(type.isInstance(value)) {
                log.debug("Cache hit for key: {}", key);
                return Optional.of(type.cast(value));
            } else {
                log.debug("Cache miss for key: {}", key);
                return Optional.empty();
            }
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable while GET key={} -> {}", key, e.getMessage());
            return Optional.empty();
        } catch (DataAccessException e) {
            log.error("Redis data access error while GET key={} -> {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, Object value, long ttlSeconds) {
        try{
            ValueOperations<String, Object> ops = redisTemplate.opsForValue();
            ops.set(key, value, Duration.ofSeconds(ttlSeconds));
            log.debug("Cache put key={} with TTL={}s", key, ttlSeconds);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable while PUT key={} -> {}", key, e.getMessage());
        } catch (DataAccessException e) {
            log.error("Redis data access error while PUT key={} -> {}", key, e.getMessage());
        }
    }

    @Override
    public boolean delete(String key) {
        try{
            Boolean result = redisTemplate.delete(key);
            log.debug("Cache delete key={} result={}", key, result);
            return result != null && result;
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable while DELETE key={} -> {}", key, e.getMessage());
            return false;
        } catch (DataAccessException e) {
            log.error("Redis data access error while DELETE key={} -> {}", key, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exists(String key) {
        try{
            Boolean result = redisTemplate.hasKey(key);
            return result != null && result;
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable while EXISTS key={} -> {}", key, e.getMessage());
            return false;
        } catch (DataAccessException e) {
            log.error("Redis data access error while EXISTS key={} -> {}", key, e.getMessage());
            return false;
        }
    }

    @Override
    public <T> Optional<T> getUrlMapping(String shortCode, Class<T> type) {
        return get(URL_PREFIX + shortCode, type);
    }

    @Override
    public void putUrlMapping(String shortCode, Object urlMapping, long ttlSeconds) {
        put(URL_PREFIX + shortCode, urlMapping, ttlSeconds);
    }

    @Override
    public boolean deleteUrlMapping(String shortCode) {
        return delete(URL_PREFIX + shortCode);
    }

    @Override
    public boolean existsUrlMapping(String shortCode) {
        return exists(URL_PREFIX + shortCode);
    }
}
