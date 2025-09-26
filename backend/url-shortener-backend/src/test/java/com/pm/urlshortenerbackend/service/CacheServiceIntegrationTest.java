package com.pm.urlshortenerbackend.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Author: sathwikpillalamarri
 * Date: 9/26/25
 * Project: url-shortener-backend
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.data.redis.port=6370",
        "spring.data.redis.host=localhost"
})
@DirtiesContext
public class CacheServiceIntegrationTest {
    private static RedisServer redisServer;

    @Autowired
    private CacheService cacheService;

    @BeforeAll
    static void startRedis() throws IOException{
        redisServer = new RedisServer(6370);
        redisServer.start();
    }

    @AfterAll
    static void stopRedis() {
        redisServer.stop();
    }

    @Test
    void testCachePutAndGet() {
        cacheService.put("testKey", "testValue", 1);

        Optional<String> value = cacheService.get("testKey", String.class);

        assertTrue(value.isPresent());
        assertEquals("testValue", value.get());
    }

    @Test
    void testTTLExpiration() throws InterruptedException {
        cacheService.put("tempKey", "tempValue", 1);
        Thread.sleep(2000);

        Optional<String> value = cacheService.get("tempKey", String.class);

        assertFalse(value.isPresent());
    }

    @Test
    void testUrlMappingOperations() {
        cacheService.putUrlMapping("abc123", "http://example.com", 60);

        Optional<String> result = cacheService.getUrlMapping("abc123", String.class);
        assertTrue(result.isPresent());
        assertEquals("http://example.com", result.get());

        assertTrue(cacheService.existsUrlMapping("abc123"));
        assertTrue(cacheService.deleteUrlMapping("abc123"));
        assertFalse(cacheService.existsUrlMapping("abc123"));
    }
}
