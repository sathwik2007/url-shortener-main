package com.pm.urlshortenerbackend.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import redis.embedded.RedisServer;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Author: sathwikpillalamarri
 * Date: 9/25/25
 * Project: url-shortener-backend
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.data.redis.port=6370",
        "spring.data.redis.host=localhost",
        "app.cache.ttl=2"
})
@DirtiesContext
public class RedisConfigTest {
    private static RedisServer redisServer;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheManager cacheManager;

    @BeforeAll
    static void startRedis() throws IOException {
        redisServer = new RedisServer(6370);
        redisServer.start();
    }

    @AfterAll
    static void stopRedis() {
        redisServer.stop();
    }

    @Test
    void testRedisTemplate() {
        redisTemplate.opsForValue().set("testKey", "testValue");
        String value = (String) redisTemplate.opsForValue().get("testKey");
        assertEquals("testValue", value);
    }

    @Test
    void testCacheManager() {
        Cache cache = cacheManager.getCache("testCache");
        cache.put("key1", "value1");
        assertEquals("value1", cache.get("key1").get());
    }
}
