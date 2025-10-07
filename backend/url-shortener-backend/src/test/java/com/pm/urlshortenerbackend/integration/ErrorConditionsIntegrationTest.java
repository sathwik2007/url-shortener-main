package com.pm.urlshortenerbackend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.urlshortenerbackend.dto.CreateUrlRequest;
import com.pm.urlshortenerbackend.exception.InvalidUrlException;
import com.pm.urlshortenerbackend.exception.UrlNotFoundException;
import com.pm.urlshortenerbackend.model.UrlMapping;
import com.pm.urlshortenerbackend.repository.UrlMappingRepository;
import com.pm.urlshortenerbackend.service.UrlService;
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
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for error conditions and edge cases
 * Tests various failure scenarios and system resilience
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Transactional
public class ErrorConditionsIntegrationTest {

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
    private UrlService urlService;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        urlMappingRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    // ========== URL Validation Error Tests ==========

    @Test
    void testInvalidUrl_MalformedUrl() throws Exception {
        String[] invalidUrls = {
                "not-a-url",
                "ftp://example.com",
                "javascript:alert('xss')",
                "data:text/html,<script>alert('xss')</script>",
                "file:///etc/passwd",
                "mailto:test@example.com",
                "tel:+1234567890",
                "",
                "   ",
                "http://",
                "https://",
                "http:///path",
                "https:///path"
        };

        for (String invalidUrl : invalidUrls) {
            CreateUrlRequest request = new CreateUrlRequest();
            request.setOriginalUrl(invalidUrl);

            mockMvc.perform(post("/api/links")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        // Verify no database entries were created
        assertThat(urlMappingRepository.count()).isEqualTo(0);
    }

    @Test
    void testInvalidUrl_TooLongUrl() throws Exception {
        // Create URL exceeding maximum length (2048 characters)
        String longPath = "a".repeat(2100);
        String tooLongUrl = "https://www.example.com/" + longPath;
        
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl(tooLongUrl);

        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.originalUrl").exists()); // Validation error

        // Verify no database entry was created
        assertThat(urlMappingRepository.count()).isEqualTo(0);
    }

    @Test
    void testInvalidUrl_ServiceLayerValidation() {
        // Test service layer validation directly
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("invalid-url-format");

        assertThatThrownBy(() -> urlService.createShortUrl(request))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("Malformed URL");
    }

    // ========== Short Code Not Found Tests ==========

    @Test
    void testShortCodeNotFound_NonExistentCode() throws Exception {
        String[] nonExistentCodes = {
                "notfound",
                "missing123",
                "xyz789",
                "1234567890",
                "abcdefghij"
        };

        for (String shortCode : nonExistentCodes) {
            mockMvc.perform(get("/" + shortCode))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.error").value("Short code not found: " + shortCode));
        }
    }

    @Test
    void testShortCodeNotFound_ServiceLayer() {
        // Test service layer exception directly
        assertThatThrownBy(() -> urlService.getOriginalUrl("nonexistent"))
                .isInstanceOf(UrlNotFoundException.class)
                .hasMessageContaining("Short code not found");
    }

    @Test
    void testShortCodeNotFound_InvalidFormat() throws Exception {
        String[] invalidFormats = {
                "abc@123",      // Special character
                "abc 123",      // Space
                "abc-123",      // Hyphen
                "abc_123",      // Underscore
                "abc#123",      // Hash
                "abc.123",      // Dot
                "abc/123",      // Slash
                "abc\\123",     // Backslash
                "abc%123",      // Percent
                "abc+123",      // Plus
                "abc=123",      // Equals
                "abc?123",      // Question mark
                "abc&123",      // Ampersand
                "verylongshortcode123456789" // Too long (>10 chars)
        };

        for (String invalidFormat : invalidFormats) {
            mockMvc.perform(get("/" + invalidFormat))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.originalUrl").exists()); // Validation error
        }
    }

    // ========== Concurrent Access Tests ==========

    @Test
    void testConcurrentUrlCreation_SameUrl() throws Exception {
        String originalUrl = "https://www.example.com/concurrent-test";
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        CompletableFuture<String>[] futures = new CompletableFuture[threadCount];

        // Submit concurrent requests for the same URL
        for (int i = 0; i < threadCount; i++) {
            futures[i] = CompletableFuture.supplyAsync(() -> {
                try {
                    CreateUrlRequest request = new CreateUrlRequest();
                    request.setOriginalUrl(originalUrl);
                    var response = urlService.createShortUrl(request);
                    return response.getShortCode();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
        }

        // Wait for all requests to complete
        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);

        // Collect all short codes
        String[] shortCodes = new String[threadCount];
        for (int i = 0; i < threadCount; i++) {
            shortCodes[i] = futures[i].get();
        }

        // All requests should return the same short code (duplicate URL handling)
        String firstShortCode = shortCodes[0];
        for (String shortCode : shortCodes) {
            assertThat(shortCode).isEqualTo(firstShortCode);
        }

        // Verify only one database entry exists
        assertThat(urlMappingRepository.count()).isEqualTo(1);

        executor.shutdown();
    }

    @Test
    void testConcurrentUrlCreation_DifferentUrls() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        CompletableFuture<String>[] futures = new CompletableFuture[threadCount];

        // Submit concurrent requests for different URLs
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures[i] = CompletableFuture.supplyAsync(() -> {
                try {
                    CreateUrlRequest request = new CreateUrlRequest();
                    request.setOriginalUrl("https://www.example.com/concurrent-test-" + index);
                    var response = urlService.createShortUrl(request);
                    return response.getShortCode();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
        }

        // Wait for all requests to complete
        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);

        // Collect all short codes
        String[] shortCodes = new String[threadCount];
        for (int i = 0; i < threadCount; i++) {
            shortCodes[i] = futures[i].get();
        }

        // All short codes should be unique
        assertThat(shortCodes).doesNotHaveDuplicates();

        // Verify all database entries exist
        assertThat(urlMappingRepository.count()).isEqualTo(threadCount);

        executor.shutdown();
    }

    @Test
    void testConcurrentRedirects_SameShortCode() throws Exception {
        // Create URL mapping
        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode("concurrent001");
        mapping.setOriginalUrl("https://www.example.com/concurrent-redirect-test");
        urlMappingRepository.save(mapping);

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        CompletableFuture<String>[] futures = new CompletableFuture[threadCount];

        // Submit concurrent redirect requests
        for (int i = 0; i < threadCount; i++) {
            futures[i] = CompletableFuture.supplyAsync(() -> {
                try {
                    return urlService.getOriginalUrl("concurrent001");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
        }

        // Wait for all requests to complete
        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);

        // All requests should return the same URL
        String expectedUrl = "https://www.example.com/concurrent-redirect-test";
        for (CompletableFuture<String> future : futures) {
            assertThat(future.get()).isEqualTo(expectedUrl);
        }

        // Verify cache entry exists
        String cacheKey = "url:concurrent001";
        assertThat(redisTemplate.hasKey(cacheKey)).isTrue();

        executor.shutdown();
    }

    // ========== System Resilience Tests ==========

    @Test
    void testSystemResilience_DatabaseRecovery() {
        // Create URL mapping
        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode("resilience001");
        mapping.setOriginalUrl("https://www.example.com/resilience-test");
        urlMappingRepository.save(mapping);

        // First access to populate cache
        String retrievedUrl = urlService.getOriginalUrl("resilience001");
        assertThat(retrievedUrl).isEqualTo("https://www.example.com/resilience-test");

        // Simulate database issue by deleting the record
        urlMappingRepository.deleteAll();

        // Should still work from cache
        String cachedUrl = urlService.getOriginalUrl("resilience001");
        assertThat(cachedUrl).isEqualTo("https://www.example.com/resilience-test");
    }

    @Test
    void testSystemResilience_CacheRecovery() {
        // Create URL mapping
        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode("resilience002");
        mapping.setOriginalUrl("https://www.example.com/cache-recovery-test");
        urlMappingRepository.save(mapping);

        // Clear cache to simulate Redis issue
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        // Should still work from database
        String retrievedUrl = urlService.getOriginalUrl("resilience002");
        assertThat(retrievedUrl).isEqualTo("https://www.example.com/cache-recovery-test");

        // Verify cache was repopulated
        String cacheKey = "url:resilience002";
        assertThat(redisTemplate.hasKey(cacheKey)).isTrue();
    }

    // ========== Edge Case Tests ==========

    @Test
    void testEdgeCase_EmptyDatabase() throws Exception {
        // Ensure database is empty
        assertThat(urlMappingRepository.count()).isEqualTo(0);

        // Try to access non-existent short code
        mockMvc.perform(get("/empty001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testEdgeCase_VeryShortShortCode() throws Exception {
        // Test single character short codes
        String[] singleCharCodes = {"a", "A", "1", "z", "Z", "9"};

        for (String shortCode : singleCharCodes) {
            // Create mapping
            UrlMapping mapping = new UrlMapping();
            mapping.setShortCode(shortCode);
            mapping.setOriginalUrl("https://www.example.com/single-char-" + shortCode);
            urlMappingRepository.save(mapping);

            // Test redirect
            mockMvc.perform(get("/" + shortCode))
                    .andExpect(status().isMovedPermanently())
                    .andExpect(header().string("Location", "https://www.example.com/single-char-" + shortCode));
        }
    }

    @Test
    void testEdgeCase_MaxLengthShortCode() throws Exception {
        // Test maximum length short code (10 characters)
        String maxLengthCode = "1234567890";
        
        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode(maxLengthCode);
        mapping.setOriginalUrl("https://www.example.com/max-length-test");
        urlMappingRepository.save(mapping);

        mockMvc.perform(get("/" + maxLengthCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://www.example.com/max-length-test"));
    }

    @Test
    void testEdgeCase_UnicodeInUrl() throws Exception {
        // Test URLs with Unicode characters
        String unicodeUrl = "https://www.example.com/测试/café/naïve?query=résumé";
        
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl(unicodeUrl);

        var result = mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        var response = objectMapper.readValue(responseBody, com.pm.urlshortenerbackend.dto.CreateUrlResponse.class);

        // Test redirect preserves Unicode
        mockMvc.perform(get("/" + response.getShortCode()))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", unicodeUrl));
    }

    @Test
    void testEdgeCase_VeryLongQueryParameters() throws Exception {
        // Test URL with very long query parameters
        String longParam = "param=" + "a".repeat(1000);
        String longQueryUrl = "https://www.example.com/test?" + longParam;
        
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl(longQueryUrl);

        var result = mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        var response = objectMapper.readValue(responseBody, com.pm.urlshortenerbackend.dto.CreateUrlResponse.class);

        // Test redirect preserves long query parameters
        mockMvc.perform(get("/" + response.getShortCode()))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", longQueryUrl));
    }

    // ========== Malformed Request Tests ==========

    @Test
    void testMalformedRequest_InvalidJson() throws Exception {
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMalformedRequest_MissingContentType() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        mockMvc.perform(post("/api/links")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void testMalformedRequest_WrongHttpMethod() throws Exception {
        // Test wrong HTTP methods
        mockMvc.perform(get("/api/links"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(post("/nonexistent"))
                .andExpect(status().isNotFound());
    }

    // ========== Resource Exhaustion Tests ==========

    @Test
    void testResourceExhaustion_ManyUrls() throws Exception {
        // Create many URLs to test system behavior under load
        int urlCount = 100;
        
        for (int i = 0; i < urlCount; i++) {
            String originalUrl = "https://www.example.com/load-test-" + i;
            CreateUrlRequest request = new CreateUrlRequest();
            request.setOriginalUrl(originalUrl);

            mockMvc.perform(post("/api/links")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Verify all URLs were created
        assertThat(urlMappingRepository.count()).isEqualTo(urlCount);

        // Test that all redirects still work
        var allMappings = urlMappingRepository.findAll();
        for (UrlMapping mapping : allMappings) {
            mockMvc.perform(get("/" + mapping.getShortCode()))
                    .andExpect(status().isMovedPermanently())
                    .andExpect(header().string("Location", mapping.getOriginalUrl()));
        }
    }
}