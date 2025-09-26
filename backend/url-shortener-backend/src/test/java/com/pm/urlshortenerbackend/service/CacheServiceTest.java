package com.pm.urlshortenerbackend.service;

import com.pm.urlshortenerbackend.service.impl.CacheServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Author: sathwikpillalamarri
 * Date: 9/26/25
 * Project: url-shortener-backend
 */
public class CacheServiceTest {
    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOps;
    private CacheService cacheService;

    @BeforeEach
    void setup() {
        redisTemplate = Mockito.mock(RedisTemplate.class);
        valueOps = Mockito.mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        cacheService = new CacheServiceImpl(redisTemplate);
    }

    @Test
    void testGetFromCache_Success() {
        when(valueOps.get("key1")).thenReturn("value1");

        Optional<String> result = cacheService.get("key1", String.class);

        assertTrue(result.isPresent());
        assertEquals("value1", result.get());

        verify(valueOps, times(1)).get("key1");
    }

    @Test
    void testGetFromCache_CacheMiss() {
        when(valueOps.get("key1")).thenReturn(null);

        Optional<String> result = cacheService.get("key1", String.class);

        assertFalse(result.isPresent());
    }

    @Test
    void testGetFromCache_RedisConnectionFailure() {
        when(valueOps.get("key1")).thenThrow(new RedisConnectionFailureException("Redis down"));

        Optional<String> result = cacheService.get("key1", String.class);

        assertFalse(result.isPresent());
    }

    @Test
    void testPutToCache_Success() {
        cacheService.put("key1", "value1", 60);

        verify(valueOps, times(1)).set(eq("key1"), eq("value1"), any());
    }

    @Test
    void testPutToCache_RedisConnectionFailure() {
        doThrow(new RedisConnectionFailureException("Redis down")).when(valueOps).set(anyString(), any(), any(Duration.class));

        assertDoesNotThrow(() -> cacheService.put("key1", "value1", 60));
    }

    @Test
    void testDeleteFromCache_Success() {
        when(redisTemplate.delete("key1")).thenReturn(true);

        boolean result = cacheService.delete("key1");

        assertTrue(result);
        verify(redisTemplate, times(1)).delete("key1");
    }

    @Test
    void testExistsInCache_Success() {
        when(redisTemplate.hasKey("key1")).thenReturn(true);

        boolean result = cacheService.exists("key1");

        assertTrue(result);
        verify(redisTemplate, times(1)).hasKey("key1");
    }

    @Test
    void testGetUrlMapping_Success() {
        when(valueOps.get("url:abc123")).thenReturn("http://example.com");

        Optional<String> result = cacheService.getUrlMapping("abc123", String.class);

        assertTrue(result.isPresent());
        assertEquals("http://example.com", result.get());
    }

    @Test
    void testGetFromCache_TypeMismatch() {
        when(valueOps.get("key1")).thenReturn(123);

        Optional<String> result = cacheService.get("key1", String.class);

        assertFalse(result.isPresent());
    }
}
