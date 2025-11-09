package com.pm.urlshortenerbackend.integration;

import com.pm.urlshortenerbackend.dto.CreateUrlRequest;
import com.pm.urlshortenerbackend.dto.CreateUrlResponse;
import com.pm.urlshortenerbackend.dto.UrlMappingResponse;
import com.pm.urlshortenerbackend.exception.UnauthorizedAccessException;
import com.pm.urlshortenerbackend.model.User;
import com.pm.urlshortenerbackend.model.UrlMapping;
import com.pm.urlshortenerbackend.repository.UrlMappingRepository;
import com.pm.urlshortenerbackend.repository.UserRepository;
import com.pm.urlshortenerbackend.service.UrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for UrlService user ownership functionality
 * Tests the complete flow from user creation to URL management
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UrlServiceUserOwnershipIntegrationTest {

    @Autowired
    private UrlService urlService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        urlMappingRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        testUser1 = new User();
        testUser1.setEmail("user1@example.com");
        testUser1.setPasswordHash("hashedPassword1");
        testUser1.setFirstName("Test");
        testUser1.setLastName("User1");
        testUser1 = userRepository.save(testUser1);

        testUser2 = new User();
        testUser2.setEmail("user2@example.com");
        testUser2.setPasswordHash("hashedPassword2");
        testUser2.setFirstName("Test");
        testUser2.setLastName("User2");
        testUser2 = userRepository.save(testUser2);
    }

    @Test
    void testCompleteUserUrlFlow() {
        // Test 1: Create URL with user ownership
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        CreateUrlResponse response = urlService.createShortUrl(request, testUser1);

        assertNotNull(response);
        assertNotNull(response.getShortCode());
        assertEquals("https://www.example.com", response.getOriginalUrl());

        // Verify URL is saved in database with correct owner
        Optional<UrlMapping> savedMapping = urlMappingRepository.findByShortCode(response.getShortCode());
        assertTrue(savedMapping.isPresent());
        assertEquals(testUser1.getId(), savedMapping.get().getOwner().getId());

        // Test 2: Verify ownership validation
        assertTrue(urlService.isUrlOwnedByUser(response.getShortCode(), testUser1));
        assertFalse(urlService.isUrlOwnedByUser(response.getShortCode(), testUser2));

        // Test 3: Validate ownership (should not throw)
        assertDoesNotThrow(() -> {
            urlService.validateUrlOwnership(response.getShortCode(), testUser1);
        });

        // Test 4: Validate ownership with wrong user (should throw)
        assertThrows(UnauthorizedAccessException.class, () -> {
            urlService.validateUrlOwnership(response.getShortCode(), testUser2);
        });

        // Test 5: Get user URLs
        Pageable pageable = PageRequest.of(0, 10);
        Page<UrlMappingResponse> userUrls = urlService.getUserUrls(testUser1, pageable);

        assertNotNull(userUrls);
        assertEquals(1, userUrls.getTotalElements());
        assertEquals(response.getShortCode(), userUrls.getContent().get(0).getShortCode());

        // Test 6: Verify user isolation - user2 should have no URLs
        Page<UrlMappingResponse> user2Urls = urlService.getUserUrls(testUser2, pageable);
        assertEquals(0, user2Urls.getTotalElements());

        // Test 7: URL count verification
        assertEquals(1, urlService.getUserUrlCount(testUser1));
        assertEquals(0, urlService.getUserUrlCount(testUser2));
        assertEquals(1, urlService.getUserActiveUrlCount(testUser1));
        assertEquals(0, urlService.getUserActiveUrlCount(testUser2));
    }

    @Test
    void testDuplicateUrlHandlingPerUser() {
        // Create same URL for two different users
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.duplicate-test.com");

        // User 1 creates URL
        CreateUrlResponse response1 = urlService.createShortUrl(request, testUser1);
        assertNotNull(response1);

        // User 2 creates same URL - should get different short code
        CreateUrlResponse response2 = urlService.createShortUrl(request, testUser2);
        assertNotNull(response2);

        // Should have different short codes
        assertNotEquals(response1.getShortCode(), response2.getShortCode());

        // Both should have same original URL
        assertEquals("https://www.duplicate-test.com", response1.getOriginalUrl());
        assertEquals("https://www.duplicate-test.com", response2.getOriginalUrl());

        // Verify ownership isolation
        assertTrue(urlService.isUrlOwnedByUser(response1.getShortCode(), testUser1));
        assertFalse(urlService.isUrlOwnedByUser(response1.getShortCode(), testUser2));
        
        assertTrue(urlService.isUrlOwnedByUser(response2.getShortCode(), testUser2));
        assertFalse(urlService.isUrlOwnedByUser(response2.getShortCode(), testUser1));

        // User 1 creates same URL again - should get existing short code
        CreateUrlResponse response3 = urlService.createShortUrl(request, testUser1);
        assertEquals(response1.getShortCode(), response3.getShortCode());

        // Verify URL counts
        assertEquals(1, urlService.getUserUrlCount(testUser1));
        assertEquals(1, urlService.getUserUrlCount(testUser2));
    }

    @Test
    void testUrlExpirationWithOwnership() {
        // Create URL with expiration date
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.expiring-url.com");
        LocalDateTime expirationDate = LocalDateTime.now().plusDays(7);
        request.setExpiresAt(expirationDate);

        CreateUrlResponse response = urlService.createShortUrl(request, testUser1);
        assertNotNull(response);

        // Verify expiration date is set in database
        Optional<UrlMapping> savedMapping = urlMappingRepository.findByShortCode(response.getShortCode());
        assertTrue(savedMapping.isPresent());
        assertNotNull(savedMapping.get().getExpiresAt());
        assertEquals(expirationDate.withNano(0), savedMapping.get().getExpiresAt().withNano(0));
        assertEquals(testUser1.getId(), savedMapping.get().getOwner().getId());
    }

    @Test
    void testPaginationWithMultipleUrls() {
        // Create multiple URLs for user1
        for (int i = 1; i <= 15; i++) {
            CreateUrlRequest request = new CreateUrlRequest();
            request.setOriginalUrl("https://www.example" + i + ".com");
            urlService.createShortUrl(request, testUser1);
        }

        // Test pagination - first page
        Pageable firstPage = PageRequest.of(0, 10);
        Page<UrlMappingResponse> page1 = urlService.getUserUrls(testUser1, firstPage);

        assertEquals(10, page1.getContent().size());
        assertEquals(15, page1.getTotalElements());
        assertEquals(2, page1.getTotalPages());
        assertTrue(page1.hasNext());

        // Test pagination - second page
        Pageable secondPage = PageRequest.of(1, 10);
        Page<UrlMappingResponse> page2 = urlService.getUserUrls(testUser1, secondPage);

        assertEquals(5, page2.getContent().size());
        assertEquals(15, page2.getTotalElements());
        assertEquals(2, page2.getTotalPages());
        assertFalse(page2.hasNext());

        // Verify user2 still has no URLs
        Page<UrlMappingResponse> user2Urls = urlService.getUserUrls(testUser2, firstPage);
        assertEquals(0, user2Urls.getTotalElements());
    }

    @Test
    void testActiveUrlFiltering() {
        // Create active URL
        CreateUrlRequest activeRequest = new CreateUrlRequest();
        activeRequest.setOriginalUrl("https://www.active-url.com");
        CreateUrlResponse activeResponse = urlService.createShortUrl(activeRequest, testUser1);

        // Create inactive URL by directly manipulating database
        CreateUrlRequest inactiveRequest = new CreateUrlRequest();
        inactiveRequest.setOriginalUrl("https://www.inactive-url.com");
        CreateUrlResponse inactiveResponse = urlService.createShortUrl(inactiveRequest, testUser1);

        // Make second URL inactive
        Optional<UrlMapping> inactiveMapping = urlMappingRepository.findByShortCode(inactiveResponse.getShortCode());
        assertTrue(inactiveMapping.isPresent());
        inactiveMapping.get().setIsActive(false);
        urlMappingRepository.save(inactiveMapping.get());

        // Test all URLs
        Pageable pageable = PageRequest.of(0, 10);
        Page<UrlMappingResponse> allUrls = urlService.getUserUrls(testUser1, pageable);
        assertEquals(2, allUrls.getTotalElements());

        // Test active URLs only
        Page<UrlMappingResponse> activeUrls = urlService.getUserActiveUrls(testUser1, pageable);
        assertEquals(1, activeUrls.getTotalElements());
        assertEquals(activeResponse.getShortCode(), activeUrls.getContent().get(0).getShortCode());

        // Verify counts
        assertEquals(2, urlService.getUserUrlCount(testUser1));
        assertEquals(1, urlService.getUserActiveUrlCount(testUser1));
    }

    @Test
    void testBackwardCompatibilityWithAnonymousUrls() {
        // Create anonymous URL (no user)
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.anonymous-url.com");

        CreateUrlResponse anonymousResponse = urlService.createShortUrl(request);
        assertNotNull(anonymousResponse);

        // Verify URL is saved without owner
        Optional<UrlMapping> savedMapping = urlMappingRepository.findByShortCode(anonymousResponse.getShortCode());
        assertTrue(savedMapping.isPresent());
        assertNull(savedMapping.get().getOwner());

        // Verify no user owns this URL
        assertFalse(urlService.isUrlOwnedByUser(anonymousResponse.getShortCode(), testUser1));
        assertFalse(urlService.isUrlOwnedByUser(anonymousResponse.getShortCode(), testUser2));

        // Create user URL with same original URL - should create new short code
        CreateUrlResponse userResponse = urlService.createShortUrl(request, testUser1);
        assertNotNull(userResponse);
        assertNotEquals(anonymousResponse.getShortCode(), userResponse.getShortCode());

        // Verify user owns their URL but not the anonymous one
        assertTrue(urlService.isUrlOwnedByUser(userResponse.getShortCode(), testUser1));
        assertFalse(urlService.isUrlOwnedByUser(anonymousResponse.getShortCode(), testUser1));
    }

    @Test
    void testErrorHandlingAndEdgeCases() {
        // Test null user validation
        assertThrows(UnauthorizedAccessException.class, () -> {
            urlService.validateUrlOwnership("any-code", null);
        });

        // Test null owner for getUserUrls
        assertThrows(IllegalArgumentException.class, () -> {
            urlService.getUserUrls(null, PageRequest.of(0, 10));
        });

        // Test null owner for getUserActiveUrls
        assertThrows(IllegalArgumentException.class, () -> {
            urlService.getUserActiveUrls(null, PageRequest.of(0, 10));
        });

        // Test null owner for getUserUrlCount
        assertThrows(IllegalArgumentException.class, () -> {
            urlService.getUserUrlCount(null);
        });

        // Test null owner for getUserActiveUrlCount
        assertThrows(IllegalArgumentException.class, () -> {
            urlService.getUserActiveUrlCount(null);
        });

        // Test ownership validation with non-existent URL
        assertThrows(UnauthorizedAccessException.class, () -> {
            urlService.validateUrlOwnership("non-existent", testUser1);
        });
    }

    @Test
    void testConcurrentUserOperations() {
        // This test simulates concurrent operations by different users
        CreateUrlRequest request1 = new CreateUrlRequest();
        request1.setOriginalUrl("https://www.concurrent1.com");

        CreateUrlRequest request2 = new CreateUrlRequest();
        request2.setOriginalUrl("https://www.concurrent2.com");

        // Create URLs concurrently (simulated)
        CreateUrlResponse response1 = urlService.createShortUrl(request1, testUser1);
        CreateUrlResponse response2 = urlService.createShortUrl(request2, testUser2);

        // Verify both operations succeeded
        assertNotNull(response1);
        assertNotNull(response2);
        assertNotEquals(response1.getShortCode(), response2.getShortCode());

        // Verify proper ownership
        assertTrue(urlService.isUrlOwnedByUser(response1.getShortCode(), testUser1));
        assertFalse(urlService.isUrlOwnedByUser(response1.getShortCode(), testUser2));

        assertTrue(urlService.isUrlOwnedByUser(response2.getShortCode(), testUser2));
        assertFalse(urlService.isUrlOwnedByUser(response2.getShortCode(), testUser1));

        // Verify user isolation in URL retrieval
        Pageable pageable = PageRequest.of(0, 10);
        
        Page<UrlMappingResponse> user1Urls = urlService.getUserUrls(testUser1, pageable);
        assertEquals(1, user1Urls.getTotalElements());
        assertEquals(response1.getShortCode(), user1Urls.getContent().get(0).getShortCode());

        Page<UrlMappingResponse> user2Urls = urlService.getUserUrls(testUser2, pageable);
        assertEquals(1, user2Urls.getTotalElements());
        assertEquals(response2.getShortCode(), user2Urls.getContent().get(0).getShortCode());
    }
}