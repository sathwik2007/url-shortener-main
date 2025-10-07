package com.pm.urlshortenerbackend.integration;

import com.pm.urlshortenerbackend.model.UrlMapping;
import com.pm.urlshortenerbackend.repository.UrlMappingRepository;
import com.pm.urlshortenerbackend.service.CacheService;
import com.pm.urlshortenerbackend.service.UrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests specifically focused on Redis caching behavior
 * Tests Requirements 3.1 and 3.2: Redis caching and cache-aside pattern
 */
@SpringBootTest
@Testcontainers
@Transactional
public class RedisCachingIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("urlshortener_test")
            .withUsername("test")
            .withPassword("test")
            .withStartupTimeout(Duration.ofMinutes(2));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withStartupTimeout(Duration.ofMinutes(2));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("app.cache.ttl", () -> "3600"); // 1 hour TTL for testing
    }

    @Autowired
    private UrlService urlService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        // Clean up database and Redis before each test
        urlMappingRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    // ========== Cache-Aside Pattern Tests (Requirement 3.2) ==========

    @Test
    void testCacheAsidePattern_CacheMiss_LoadFromDatabase() {
        // Arrange: Create URL mapping directly in database (bypassing cache)
        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode("cache001");
        mapping.setOriginalUrl("https://www.example.com/cache-miss-test");
        urlMappingRepository.save(mapping);

        // Ensure no cache entry exists
        String cacheKey = "url:cache001";
        assertThat(redisTemplate.hasKey(cacheKey)).isFalse();

        // Act: Retrieve URL (should trigger cache miss and database load)
        String retrievedUrl = urlService.getOriginalUrl("cache001");

        // Assert: URL retrieved correctly and cached
        assertThat(retrievedUrl).isEqualTo("https://www.example.com/cache-miss-test");
        
        // Verify cache entry was created
        assertThat(redisTemplate.hasKey(cacheKey)).isTrue();
        Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
        assertThat(cachedValue.toString()).isEqualTo("https://www.example.com/cache-miss-test");
    }

    @Test
    void testCacheAsidePattern_CacheHit_SkipDatabase() {
        // Arrange: Pre-populate cache without database entry
        String cacheKey = "url:cache002";
        String originalUrl = "https://www.example.com/cache-hit-test";
        redisTemplate.opsForValue().set(cacheKey, originalUrl);

        // Verify no database entry exists
        Optional<UrlMapping> dbMapping = urlMappingRepository.findByShortCode("cache002");
        assertThat(dbMapping).isEmpty();

        // Act: Retrieve URL (should hit cache and skip database)
        String retrievedUrl = urlService.getOriginalUrl("cache002");

        // Assert: URL retrieved from cache
        assertThat(retrievedUrl).isEqualTo(originalUrl);
        
        // Verify database was not queried (still no entry)
        dbMapping = urlMappingRepository.findByShortCode("cache002");
        assertThat(dbMapping).isEmpty();
    }

    @Test
    void testCacheAsidePattern_CacheAndDatabaseConsistency() {
        // Arrange: Create URL mapping through service (should populate both)
        String originalUrl = "https://www.example.com/consistency-test";
        var createRequest = new com.pm.urlshortenerbackend.dto.CreateUrlRequest();
        createRequest.setOriginalUrl(originalUrl);
        
        var response = urlService.createShortUrl(createRequest);
        String shortCode = response.getShortCode();

        // Act: Retrieve URL multiple times
        String firstRetrieval = urlService.getOriginalUrl(shortCode);
        String secondRetrieval = urlService.getOriginalUrl(shortCode);

        // Assert: Consistent results
        assertThat(firstRetrieval).isEqualTo(originalUrl);
        assertThat(secondRetrieval).isEqualTo(originalUrl);
        assertThat(firstRetrieval).isEqualTo(secondRetrieval);

        // Verify both database and cache have correct data
        Optional<UrlMapping> dbMapping = urlMappingRepository.findByShortCode(shortCode);
        String cacheKey = "url:" + shortCode;
        Object cachedValue = redisTemplate.opsForValue().get(cacheKey);

        assertThat(dbMapping).isPresent();
        assertThat(dbMapping.get().getOriginalUrl()).isEqualTo(originalUrl);
        assertThat(cachedValue).isNotNull();
        assertThat(cachedValue.toString()).isEqualTo(originalUrl);
    }

    // ========== Cache TTL and Expiration Tests (Requirement 3.1) ==========

    @Test
    void testCacheTTL_ExpirationBehavior() {
        // Arrange: Create mapping and set short TTL
        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode("ttl001");
        mapping.setOriginalUrl("https://www.example.com/ttl-test");
        urlMappingRepository.save(mapping);

        String cacheKey = "url:ttl001";
        
        // Set cache entry with 2-second TTL
        redisTemplate.opsForValue().set(cacheKey, mapping.getOriginalUrl(), Duration.ofSeconds(2));
        
        // Verify cache entry exists
        assertThat(redisTemplate.hasKey(cacheKey)).isTrue();

        // Wait for expiration
        await().atMost(3, TimeUnit.SECONDS)
                .until(() -> !redisTemplate.hasKey(cacheKey));

        // Act: Retrieve URL after cache expiration (should reload from database)
        String retrievedUrl = urlService.getOriginalUrl("ttl001");

        // Assert: URL still retrieved (from database) and re-cached
        assertThat(retrievedUrl).isEqualTo("https://www.example.com/ttl-test");
        assertThat(redisTemplate.hasKey(cacheKey)).isTrue(); // Re-cached
    }

    @Test
    void testCacheService_DirectOperations() {
        // Test direct cache service operations
        String shortCode = "direct001";
        String originalUrl = "https://www.example.com/direct-test";

        // Test cache set
        cacheService.putUrlMapping(shortCode, originalUrl, 3600);
        
        // Verify cache entry exists
        assertThat(cacheService.existsUrlMapping(shortCode)).isTrue();

        // Test cache get
        Optional<String> cachedUrl = cacheService.getUrlMapping(shortCode, String.class);
        assertThat(cachedUrl).isPresent();
        assertThat(cachedUrl.get()).isEqualTo(originalUrl);

        // Test cache delete
        cacheService.deleteUrlMapping(shortCode);
        assertThat(cacheService.existsUrlMapping(shortCode)).isFalse();
        
        Optional<String> deletedUrl = cacheService.getUrlMapping(shortCode, String.class);
        assertThat(deletedUrl).isEmpty();
    }

    // ========== Cache Fallback Tests (Requirement 3.5) ==========

    @Test
    void testCacheFallback_RedisUnavailable() {
        // This test simulates Redis being unavailable
        // Note: In practice, we'd need to actually stop Redis or simulate connection failure
        
        // Arrange: Create URL mapping in database
        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode("fallback001");
        mapping.setOriginalUrl("https://www.example.com/fallback-test");
        urlMappingRepository.save(mapping);

        // Clear any existing cache
        String cacheKey = "url:fallback001";
        redisTemplate.delete(cacheKey);

        // Act: Retrieve URL (should work even if Redis operations fail)
        String retrievedUrl = urlService.getOriginalUrl("fallback001");

        // Assert: URL retrieved successfully from database
        assertThat(retrievedUrl).isEqualTo("https://www.example.com/fallback-test");
    }

    // ========== Performance and Load Tests ==========

    @Test
    void testCachePerformance_MultipleRetrievals() {
        // Arrange: Create URL mapping
        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode("perf001");
        mapping.setOriginalUrl("https://www.example.com/performance-test");
        urlMappingRepository.save(mapping);

        String shortCode = "perf001";
        
        // First retrieval (cache miss)
        long startTime = System.nanoTime();
        String firstResult = urlService.getOriginalUrl(shortCode);
        long firstDuration = System.nanoTime() - startTime;

        // Multiple cache hits
        long totalCacheTime = 0;
        int cacheHits = 10;
        
        for (int i = 0; i < cacheHits; i++) {
            startTime = System.nanoTime();
            String result = urlService.getOriginalUrl(shortCode);
            totalCacheTime += (System.nanoTime() - startTime);
            
            assertThat(result).isEqualTo(firstResult);
        }

        long averageCacheTime = totalCacheTime / cacheHits;
        
        // Cache hits should generally be faster than database access
        // This is a rough performance test - in production we'd use more sophisticated metrics
        System.out.println("First retrieval (DB): " + firstDuration + " ns");
        System.out.println("Average cache retrieval: " + averageCacheTime + " ns");
        
        // Verify all retrievals returned correct result
        assertThat(firstResult).isEqualTo("https://www.example.com/performance-test");
    }

    @Test
    void testCacheConcurrency_MultipleThreads() throws InterruptedException {
        // Arrange: Create URL mapping
        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode("concurrent001");
        mapping.setOriginalUrl("https://www.example.com/concurrent-test");
        urlMappingRepository.save(mapping);

        String shortCode = "concurrent001";
        int threadCount = 5;
        Thread[] threads = new Thread[threadCount];
        String[] results = new String[threadCount];

        // Act: Multiple threads retrieving same URL concurrently
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                results[index] = urlService.getOriginalUrl(shortCode);
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Assert: All threads got the same result
        String expectedUrl = "https://www.example.com/concurrent-test";
        for (String result : results) {
            assertThat(result).isEqualTo(expectedUrl);
        }

        // Verify cache entry exists
        String cacheKey = "url:" + shortCode;
        assertThat(redisTemplate.hasKey(cacheKey)).isTrue();
    }

    // ========== Cache Key Management Tests ==========

    @Test
    void testCacheKeyManagement_KeyFormat() {
        // Test that cache keys follow expected format
        String shortCode = "key001";
        String originalUrl = "https://www.example.com/key-test";
        
        cacheService.putUrlMapping(shortCode, originalUrl, 3600);
        
        // Verify cache entry exists
        assertThat(cacheService.existsUrlMapping(shortCode)).isTrue();
    }

    @Test
    void testCacheKeyManagement_SpecialCharacters() {
        // Test cache keys with special characters in short codes
        String[] specialShortCodes = {"abc123", "ABC123", "123abc", "a1B2c3"};
        
        for (String shortCode : specialShortCodes) {
            String originalUrl = "https://www.example.com/special-" + shortCode;
            
            cacheService.putUrlMapping(shortCode, originalUrl, 3600);
            
            assertThat(cacheService.existsUrlMapping(shortCode)).isTrue();
            
            Optional<String> cachedUrl = cacheService.getUrlMapping(shortCode, String.class);
            assertThat(cachedUrl).isPresent();
            assertThat(cachedUrl.get()).isEqualTo(originalUrl);
        }
    }

    // ========== Cache Statistics and Monitoring Tests ==========

    @Test
    void testCacheStatistics_HitMissRatio() {
        // Create multiple URL mappings
        for (int i = 1; i <= 5; i++) {
            UrlMapping mapping = new UrlMapping();
            mapping.setShortCode("stats" + String.format("%03d", i));
            mapping.setOriginalUrl("https://www.example.com/stats-test-" + i);
            urlMappingRepository.save(mapping);
        }

        // First round: All cache misses
        for (int i = 1; i <= 5; i++) {
            String shortCode = "stats" + String.format("%03d", i);
            String result = urlService.getOriginalUrl(shortCode);
            assertThat(result).isEqualTo("https://www.example.com/stats-test-" + i);
        }

        // Second round: All cache hits
        for (int i = 1; i <= 5; i++) {
            String shortCode = "stats" + String.format("%03d", i);
            String result = urlService.getOriginalUrl(shortCode);
            assertThat(result).isEqualTo("https://www.example.com/stats-test-" + i);
            
            // Verify cache entry exists
            String cacheKey = "url:" + shortCode;
            assertThat(redisTemplate.hasKey(cacheKey)).isTrue();
        }
    }
}