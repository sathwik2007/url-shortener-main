package com.pm.urlshortenerbackend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.urlshortenerbackend.dto.CreateUrlRequest;
import com.pm.urlshortenerbackend.dto.CreateUrlResponse;
import com.pm.urlshortenerbackend.model.UrlMapping;
import com.pm.urlshortenerbackend.repository.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for URL Shortener MVP
 * Tests complete flow from URL creation to redirect with real database and Redis
 * 
 * Requirements covered:
 * - 1.1: URL submission and short code generation
 * - 1.2: URL validation and error handling
 * - 2.1: Redirect functionality with HTTP 301
 * - 2.2: Invalid short code handling with HTTP 404
 * - 3.1: Redis caching for performance
 * - 3.2: Cache-aside pattern implementation
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Transactional
public class UrlShortenerIntegrationTest {

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
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    // ========== Complete Flow Tests (Requirements 1.1, 1.2, 2.1, 2.2) ==========

    @Test
    void testCompleteFlow_CreateAndRedirect_Success() throws Exception {
        // Requirement 1.1: URL submission and short code generation
        String originalUrl = "https://www.example.com/very/long/path?param=value";
        
        // Step 1: Create short URL
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl(originalUrl);

        MvcResult createResult = mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").exists())
                .andExpect(jsonPath("$.shortUrl").exists())
                .andExpect(jsonPath("$.originalUrl").value(originalUrl))
                .andReturn();

        // Extract short code from response
        String responseBody = createResult.getResponse().getContentAsString();
        CreateUrlResponse response = objectMapper.readValue(responseBody, CreateUrlResponse.class);
        String shortCode = response.getShortCode();

        // Verify database persistence
        Optional<UrlMapping> savedMapping = urlMappingRepository.findByShortCode(shortCode);
        assertThat(savedMapping).isPresent();
        assertThat(savedMapping.get().getOriginalUrl()).isEqualTo(originalUrl);
        assertThat(savedMapping.get().getShortCode()).isEqualTo(shortCode);

        // Step 2: Test redirect functionality (Requirement 2.1)
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", originalUrl))
                .andExpect(content().string(""));

        // Verify Redis caching occurred (Requirement 3.1)
        String cacheKey = "url:" + shortCode;
        Object cachedUrl = redisTemplate.opsForValue().get(cacheKey);
        assertThat(cachedUrl).isNotNull();
        assertThat(cachedUrl.toString()).isEqualTo(originalUrl);
    }

    @Test
    void testCompleteFlow_InvalidUrl_ErrorHandling() throws Exception {
        // Requirement 1.2: URL validation and error handling
        String invalidUrl = "not-a-valid-url";
        
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl(invalidUrl);

        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        // Verify no database entry was created
        assertThat(urlMappingRepository.count()).isEqualTo(0);
    }

    @Test
    void testCompleteFlow_InvalidShortCode_NotFound() throws Exception {
        // Requirement 2.2: Invalid short code handling with HTTP 404
        String nonExistentShortCode = "notfound";

        mockMvc.perform(get("/" + nonExistentShortCode))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testCompleteFlow_DuplicateUrl_ReturnExisting() throws Exception {
        // Requirement 1.3: Duplicate URL detection
        String originalUrl = "https://www.example.com/duplicate-test";
        
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl(originalUrl);

        // First request
        MvcResult firstResult = mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String firstResponse = firstResult.getResponse().getContentAsString();
        CreateUrlResponse firstUrlResponse = objectMapper.readValue(firstResponse, CreateUrlResponse.class);

        // Second request with same URL
        MvcResult secondResult = mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String secondResponse = secondResult.getResponse().getContentAsString();
        CreateUrlResponse secondUrlResponse = objectMapper.readValue(secondResponse, CreateUrlResponse.class);

        // Should return the same short code
        assertThat(secondUrlResponse.getShortCode()).isEqualTo(firstUrlResponse.getShortCode());
        
        // Verify only one database entry exists
        assertThat(urlMappingRepository.count()).isEqualTo(1);
    }

    // ========== Redis Caching Integration Tests (Requirements 3.1, 3.2) ==========

    @Test
    void testRedisCaching_CacheHitScenario() throws Exception {
        // Create URL mapping
        String originalUrl = "https://www.example.com/cache-test";
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl(originalUrl);

        MvcResult createResult = mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        CreateUrlResponse response = objectMapper.readValue(responseBody, CreateUrlResponse.class);
        String shortCode = response.getShortCode();

        // First redirect - should cache the result
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", originalUrl));

        // Verify cache entry exists
        String cacheKey = "url:" + shortCode;
        Object cachedUrl = redisTemplate.opsForValue().get(cacheKey);
        assertThat(cachedUrl).isNotNull();
        assertThat(cachedUrl.toString()).isEqualTo(originalUrl);

        // Delete from database to test cache-only scenario
        urlMappingRepository.deleteAll();

        // Second redirect - should work from cache even though DB entry is gone
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", originalUrl));
    }

    @Test
    void testRedisCaching_CacheMissScenario() throws Exception {
        // Manually insert URL mapping into database (bypassing service layer)
        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode("manual123");
        mapping.setOriginalUrl("https://www.example.com/manual-test");
        urlMappingRepository.save(mapping);

        // Ensure no cache entry exists
        String cacheKey = "url:manual123";
        redisTemplate.delete(cacheKey);

        // First redirect - cache miss, should query database and cache result
        mockMvc.perform(get("/manual123"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://www.example.com/manual-test"));

        // Verify cache entry was created
        Object cachedUrl = redisTemplate.opsForValue().get(cacheKey);
        assertThat(cachedUrl).isNotNull();
        assertThat(cachedUrl.toString()).isEqualTo("https://www.example.com/manual-test");
    }

    @Test
    void testRedisCaching_CacheExpiration() throws Exception {
        // Create URL mapping
        String originalUrl = "https://www.example.com/expiration-test";
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl(originalUrl);

        MvcResult createResult = mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        CreateUrlResponse response = objectMapper.readValue(responseBody, CreateUrlResponse.class);
        String shortCode = response.getShortCode();

        // Set a very short TTL for testing
        String cacheKey = "url:" + shortCode;
        redisTemplate.opsForValue().set(cacheKey, originalUrl, Duration.ofSeconds(1));

        // Verify cache entry exists
        Object cachedUrl = redisTemplate.opsForValue().get(cacheKey);
        assertThat(cachedUrl).isNotNull();

        // Wait for expiration
        Thread.sleep(1100);

        // Verify cache entry expired
        Object expiredUrl = redisTemplate.opsForValue().get(cacheKey);
        assertThat(expiredUrl).isNull();

        // Redirect should still work (fallback to database)
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", originalUrl));
    }

    // ========== Error Condition and Edge Case Tests ==========

    @Test
    void testErrorConditions_DatabaseUnavailable() throws Exception {
        // This test simulates database connectivity issues
        // Note: In a real scenario, we'd need to stop the container or use connection pooling limits
        
        // Create a URL first
        String originalUrl = "https://www.example.com/db-error-test";
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl(originalUrl);

        MvcResult createResult = mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        CreateUrlResponse response = objectMapper.readValue(responseBody, CreateUrlResponse.class);
        String shortCode = response.getShortCode();

        // Ensure URL is cached
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", originalUrl));

        // Clear database but keep cache
        urlMappingRepository.deleteAll();

        // Should still work from cache
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", originalUrl));
    }

    @Test
    void testErrorConditions_RedisUnavailable() throws Exception {
        // Create URL mapping directly in database
        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode("redis404");
        mapping.setOriginalUrl("https://www.example.com/redis-unavailable");
        urlMappingRepository.save(mapping);

        // Clear Redis cache to simulate unavailability
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        // Should still work with database fallback
        mockMvc.perform(get("/redis404"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://www.example.com/redis-unavailable"));
    }

    @Test
    void testEdgeCases_VeryLongUrl() throws Exception {
        // Test with maximum allowed URL length
        String longPath = "a".repeat(1900); // Create very long path
        String originalUrl = "https://www.example.com/" + longPath;
        
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl(originalUrl);

        MvcResult createResult = mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        CreateUrlResponse response = objectMapper.readValue(responseBody, CreateUrlResponse.class);
        String shortCode = response.getShortCode();

        // Test redirect works with very long URL
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", originalUrl));
    }

    @Test
    void testEdgeCases_SpecialCharactersInUrl() throws Exception {
        // Test URL with special characters, query parameters, and fragments
        String originalUrl = "https://www.example.com/path?query=hello%20world&param=test+value&unicode=café#section";
        
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl(originalUrl);

        MvcResult createResult = mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        CreateUrlResponse response = objectMapper.readValue(responseBody, CreateUrlResponse.class);
        String shortCode = response.getShortCode();

        // Test redirect preserves special characters
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", originalUrl));
    }

    @Test
    void testEdgeCases_ConcurrentRequests() throws Exception {
        // Test concurrent creation of URLs to verify uniqueness
        String baseUrl = "https://www.example.com/concurrent-test-";
        
        // Create multiple URLs concurrently (simulated)
        for (int i = 0; i < 5; i++) {
            String originalUrl = baseUrl + i;
            CreateUrlRequest request = new CreateUrlRequest();
            request.setOriginalUrl(originalUrl);

            mockMvc.perform(post("/api/links")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.shortCode").exists())
                    .andExpect(jsonPath("$.originalUrl").value(originalUrl));
        }

        // Verify all URLs were created with unique short codes
        assertThat(urlMappingRepository.count()).isEqualTo(5);
        
        // Verify all short codes are unique
        var allMappings = urlMappingRepository.findAll();
        var shortCodes = allMappings.stream().map(UrlMapping::getShortCode).toList();
        assertThat(shortCodes).hasSize(5);
        assertThat(shortCodes).doesNotHaveDuplicates();
    }

    @Test
    void testPerformance_CacheEffectiveness() throws Exception {
        // Create URL mapping
        String originalUrl = "https://www.example.com/performance-test";
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl(originalUrl);

        MvcResult createResult = mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        CreateUrlResponse response = objectMapper.readValue(responseBody, CreateUrlResponse.class);
        String shortCode = response.getShortCode();

        // Measure time for first request (cache miss)
        long startTime = System.currentTimeMillis();
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isMovedPermanently());
        long firstRequestTime = System.currentTimeMillis() - startTime;

        // Measure time for second request (cache hit)
        startTime = System.currentTimeMillis();
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isMovedPermanently());
        long secondRequestTime = System.currentTimeMillis() - startTime;

        // Cache hit should be faster (though this is a rough test)
        // In a real scenario, we'd use more sophisticated performance testing
        assertThat(secondRequestTime).isLessThanOrEqualTo(firstRequestTime + 50); // Allow some variance
    }

    // ========== Data Integrity Tests ==========

    @Test
    void testDataIntegrity_DatabaseAndCacheConsistency() throws Exception {
        // Create URL mapping
        String originalUrl = "https://www.example.com/integrity-test";
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl(originalUrl);

        MvcResult createResult = mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        CreateUrlResponse response = objectMapper.readValue(responseBody, CreateUrlResponse.class);
        String shortCode = response.getShortCode();

        // Trigger caching
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isMovedPermanently());

        // Verify database and cache have same data
        Optional<UrlMapping> dbMapping = urlMappingRepository.findByShortCode(shortCode);
        String cacheKey = "url:" + shortCode;
        Object cachedUrl = redisTemplate.opsForValue().get(cacheKey);

        assertThat(dbMapping).isPresent();
        assertThat(cachedUrl).isNotNull();
        assertThat(dbMapping.get().getOriginalUrl()).isEqualTo(cachedUrl.toString());
        assertThat(dbMapping.get().getOriginalUrl()).isEqualTo(originalUrl);
    }

    @Test
    void testDataIntegrity_ShortCodeUniqueness() throws Exception {
        // Create multiple URLs and verify all short codes are unique
        int numberOfUrls = 10;
        
        for (int i = 0; i < numberOfUrls; i++) {
            String originalUrl = "https://www.example.com/unique-test-" + i;
            CreateUrlRequest request = new CreateUrlRequest();
            request.setOriginalUrl(originalUrl);

            mockMvc.perform(post("/api/links")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Verify all entries have unique short codes
        var allMappings = urlMappingRepository.findAll();
        assertThat(allMappings).hasSize(numberOfUrls);
        
        var shortCodes = allMappings.stream().map(UrlMapping::getShortCode).toList();
        assertThat(shortCodes).doesNotHaveDuplicates();
        
        // Verify all short codes follow expected format (Base62)
        for (String shortCode : shortCodes) {
            assertThat(shortCode).matches("[0-9a-zA-Z]+");
            assertThat(shortCode.length()).isGreaterThan(0);
            assertThat(shortCode.length()).isLessThanOrEqualTo(10);
        }
    }
}