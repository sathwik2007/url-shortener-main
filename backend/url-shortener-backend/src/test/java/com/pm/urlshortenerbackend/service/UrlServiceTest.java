package com.pm.urlshortenerbackend.service;

import com.pm.urlshortenerbackend.dto.CreateUrlRequest;
import com.pm.urlshortenerbackend.dto.CreateUrlResponse;
import com.pm.urlshortenerbackend.dto.UrlMappingResponse;
import com.pm.urlshortenerbackend.exception.InvalidUrlException;
import com.pm.urlshortenerbackend.exception.UnauthorizedAccessException;
import com.pm.urlshortenerbackend.exception.UrlNotFoundException;
import com.pm.urlshortenerbackend.model.UrlMapping;
import com.pm.urlshortenerbackend.model.User;
import com.pm.urlshortenerbackend.repository.UrlMappingRepository;
import com.pm.urlshortenerbackend.service.impl.IdGenerationServiceImpl;
import com.pm.urlshortenerbackend.service.impl.UrlServiceImpl;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UrlService
 * Tests both Task 5.1 (URL creation) and Task 5.2 (URL retrieval) functionality
 */
public class UrlServiceTest {

    @Mock
    private UrlMappingRepository repository;

    @Mock
    private IdGenerationServiceImpl idGenerationServiceImpl;

    @Mock
    private CacheService cacheService;

    @Mock
    private com.pm.urlshortenerbackend.health.UrlShortenerMetrics metrics;

    private UrlService urlService;

    private final String baseUrl = "http://localhost:8080";
    private final int maxLength = 2048;
    private final long cacheTtl = 3600;
    private final boolean enableDuplicateDetection = true;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock timer behavior
        Timer.Sample mockSample = mock(Timer.Sample.class);
        when(metrics.startUrlCreationTimer()).thenReturn(mockSample);
        when(metrics.startUrlRetrievalTimer()).thenReturn(mockSample);
        
        urlService = new UrlServiceImpl(
                repository,
                idGenerationServiceImpl,
                cacheService,
                metrics,
                baseUrl,
                maxLength,
                cacheTtl,
                enableDuplicateDetection
        );
    }

    // ========== Task 5.1 Tests: URL Creation ==========

    @Test
    void testCreateShortUrl_Success() {
        // Arrange
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        UrlMapping savedMapping = new UrlMapping();
        savedMapping.setId(123L);
        savedMapping.setShortCode("abc123");
        savedMapping.setOriginalUrl("https://www.example.com");
        savedMapping.setCreatedAt(LocalDateTime.now());

        when(repository.findByOriginalUrl("https://www.example.com")).thenReturn(Optional.empty());
        when(idGenerationServiceImpl.generateUniqueShortCode()).thenReturn("abc123");
        when(repository.save(any(UrlMapping.class))).thenReturn(savedMapping);

        // Act
        CreateUrlResponse response = urlService.createShortUrl(request);

        // Assert
        assertNotNull(response);
        assertEquals("abc123", response.getShortCode());

        verify(repository).save(any(UrlMapping.class));
        verify(idGenerationServiceImpl).generateUniqueShortCode();
        verify(cacheService).putUrlMapping(eq("abc123"), any(UrlMapping.class), eq(cacheTtl));
    }

    @Test
    void testCreateShortUrl_DuplicateUrl() {
        // Arrange
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        UrlMapping existingMapping = new UrlMapping();
        existingMapping.setId(456L);
        existingMapping.setShortCode("existing123");
        existingMapping.setOriginalUrl("https://www.example.com");
        existingMapping.setCreatedAt(LocalDateTime.now());

        when(repository.findByOriginalUrl("https://www.example.com")).thenReturn(Optional.of(existingMapping));

        // Act
        CreateUrlResponse response = urlService.createShortUrl(request);

        // Assert
        assertNotNull(response);
        assertEquals("existing123", response.getShortCode());
        assertEquals("http://localhost:8080/api/existing123", response.getShortUrl());
        assertEquals("https://www.example.com", response.getOriginalUrl());

        verify(repository, never()).save(any(UrlMapping.class));
        verify(idGenerationServiceImpl, never()).generateUniqueShortCode();
        verify(cacheService).putUrlMapping("existing123", existingMapping, cacheTtl);
    }

    @Test
    void testCreateShortUrl_InvalidUrl_Null() {
        // Arrange
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl(null);

        // Act & Assert
        InvalidUrlException exception = assertThrows(InvalidUrlException.class, () -> {
            urlService.createShortUrl(request);
        });

        assertEquals("URL is null or exceeds max length", exception.getMessage());
        verify(repository, never()).findByOriginalUrl(anyString());
        verify(repository, never()).save(any(UrlMapping.class));
    }

    @Test
    void testCreateShortUrl_InvalidUrl_TooLong() {
        // Arrange
        CreateUrlRequest request = new CreateUrlRequest();
        String longUrl = "https://www.example.com/" + "a".repeat(2048);
        request.setOriginalUrl(longUrl);

        // Act & Assert
        InvalidUrlException exception = assertThrows(InvalidUrlException.class, () -> {
            urlService.createShortUrl(request);
        });

        assertEquals("URL is null or exceeds max length", exception.getMessage());
    }

    @Test
    void testCreateShortUrl_InvalidUrl_BadProtocol() {
        // Arrange
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("ftp://www.example.com");

        // Act & Assert
        InvalidUrlException exception = assertThrows(InvalidUrlException.class, () -> {
            urlService.createShortUrl(request);
        });

        assertTrue(exception.getMessage().contains("Invalid Protocol"));
    }

    @Test
    void testCreateShortUrl_InvalidUrl_Malformed() {
        // Arrange
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("not-a-valid-url");

        // Act & Assert
        InvalidUrlException exception = assertThrows(InvalidUrlException.class, () -> {
            urlService.createShortUrl(request);
        });

        assertTrue(exception.getMessage().contains("Malformed URL"));
    }

    // ========== Task 5.2 Tests: URL Retrieval ==========

    @Test
    void testGetOriginalUrl_CacheHit() {
        // Arrange
        String shortCode = "abc123";
        UrlMapping cachedMapping = new UrlMapping();
        cachedMapping.setShortCode(shortCode);
        cachedMapping.setOriginalUrl("https://www.example.com");
        cachedMapping.setCreatedAt(LocalDateTime.now());

        when(cacheService.getUrlMapping(shortCode, UrlMapping.class)).thenReturn(Optional.of(cachedMapping));

        // Act
        String originalUrl = urlService.getOriginalUrl(shortCode);

        // Assert
        assertEquals("https://www.example.com", originalUrl);
        verify(cacheService).getUrlMapping(shortCode, UrlMapping.class);
        verify(repository, never()).findByShortCode(anyString());
    }

    @Test
    void testGetOriginalUrl_CacheMiss() {
        // Arrange
        String shortCode = "abc123";
        UrlMapping dbMapping = new UrlMapping();
        dbMapping.setShortCode(shortCode);
        dbMapping.setOriginalUrl("https://www.example.com");
        dbMapping.setCreatedAt(LocalDateTime.now());

        when(cacheService.getUrlMapping(shortCode, UrlMapping.class)).thenReturn(Optional.empty());
        when(repository.findByShortCode(shortCode)).thenReturn(Optional.of(dbMapping));

        // Act
        String originalUrl = urlService.getOriginalUrl(shortCode);

        // Assert
        assertEquals("https://www.example.com", originalUrl);
        verify(cacheService).getUrlMapping(shortCode, UrlMapping.class);
        verify(repository).findByShortCode(shortCode);
        verify(cacheService).putUrlMapping(shortCode, dbMapping, cacheTtl);
    }

    @Test
    void testGetOriginalUrl_NotFound() {
        // Arrange
        String shortCode = "nonexistent";

        when(cacheService.getUrlMapping(shortCode, UrlMapping.class)).thenReturn(Optional.empty());
        when(repository.findByShortCode(shortCode)).thenReturn(Optional.empty());

        // Act & Assert
        UrlNotFoundException exception = assertThrows(UrlNotFoundException.class, () -> {
            urlService.getOriginalUrl(shortCode);
        });

        assertTrue(exception.getMessage().contains(shortCode));
        verify(cacheService).getUrlMapping(shortCode, UrlMapping.class);
        verify(repository).findByShortCode(shortCode);
        verify(cacheService, never()).putUrlMapping(anyString(), any(), anyLong());
    }

    @Test
    void testGetUrlMapping_CacheHit() {
        // Arrange
        String shortCode = "abc123";
        UrlMapping cachedMapping = new UrlMapping();
        cachedMapping.setShortCode(shortCode);
        cachedMapping.setOriginalUrl("https://www.example.com");
        cachedMapping.setCreatedAt(LocalDateTime.now());

        when(cacheService.getUrlMapping(shortCode, UrlMapping.class)).thenReturn(Optional.of(cachedMapping));

        // Act
        UrlMappingResponse response = urlService.getUrlMapping(shortCode);

        // Assert
        assertNotNull(response);
        assertEquals(shortCode, response.getShortCode());
        assertEquals("http://localhost:8080/abc123", response.getShortUrl());
        assertEquals("https://www.example.com", response.getOriginalUrl());
        assertNotNull(response.getCreatedAt());

        verify(cacheService).getUrlMapping(shortCode, UrlMapping.class);
        verify(repository, never()).findByShortCode(anyString());
    }

    @Test
    void testGetUrlMapping_CacheMiss() {
        // Arrange
        String shortCode = "abc123";
        UrlMapping dbMapping = new UrlMapping();
        dbMapping.setShortCode(shortCode);
        dbMapping.setOriginalUrl("https://www.example.com");
        dbMapping.setCreatedAt(LocalDateTime.now());

        when(cacheService.getUrlMapping(shortCode, UrlMapping.class)).thenReturn(Optional.empty());
        when(repository.findByShortCode(shortCode)).thenReturn(Optional.of(dbMapping));

        // Act
        UrlMappingResponse response = urlService.getUrlMapping(shortCode);

        // Assert
        assertNotNull(response);
        assertEquals(shortCode, response.getShortCode());
        assertEquals("http://localhost:8080/abc123", response.getShortUrl());
        assertEquals("https://www.example.com", response.getOriginalUrl());
        assertNotNull(response.getCreatedAt());

        verify(cacheService).getUrlMapping(shortCode, UrlMapping.class);
        verify(repository).findByShortCode(shortCode);
        verify(cacheService).putUrlMapping(shortCode, dbMapping, cacheTtl);
    }

    @Test
    void testGetUrlMapping_NotFound() {
        // Arrange
        String shortCode = "nonexistent";

        when(cacheService.getUrlMapping(shortCode, UrlMapping.class)).thenReturn(Optional.empty());
        when(repository.findByShortCode(shortCode)).thenReturn(Optional.empty());

        // Act & Assert
        UrlNotFoundException exception = assertThrows(UrlNotFoundException.class, () -> {
            urlService.getUrlMapping(shortCode);
        });

        assertTrue(exception.getMessage().contains(shortCode));
        verify(cacheService).getUrlMapping(shortCode, UrlMapping.class);
        verify(repository).findByShortCode(shortCode);
        verify(cacheService, never()).putUrlMapping(anyString(), any(), anyLong());
    }

    // ========== Edge Cases and Integration Tests ==========

    @Test
    void testCreateShortUrl_DuplicateDetectionDisabled() {
        // Arrange - Create service with duplicate detection disabled
        urlService = new UrlServiceImpl(
                repository,
                idGenerationServiceImpl,
                cacheService,
                metrics,
                baseUrl,
                maxLength,
                cacheTtl,
                false // Disable duplicate detection
        );

        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        UrlMapping savedMapping = new UrlMapping();
        savedMapping.setId(123L);
        savedMapping.setShortCode("abc123");
        savedMapping.setOriginalUrl("https://www.example.com");
        savedMapping.setCreatedAt(LocalDateTime.now());

        when(idGenerationServiceImpl.generateUniqueShortCode()).thenReturn("abc123");
        when(repository.save(any(UrlMapping.class))).thenReturn(savedMapping);

        // Act
        CreateUrlResponse response = urlService.createShortUrl(request);

        // Assert
        assertNotNull(response);
        assertEquals("abc123", response.getShortCode());
        
        // Verify duplicate detection was skipped
        verify(repository, never()).findByOriginalUrl(anyString());
        verify(repository, never()).findByOriginalUrlAndOwner(anyString(), any());
        verify(repository).save(any(UrlMapping.class));
    }

    @Test
    void testValidUrl_HttpsProtocol() {
        // Arrange
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://secure.example.com/path?param=value");

        UrlMapping savedMapping = new UrlMapping();
        savedMapping.setId(123L);
        savedMapping.setShortCode("abc123");
        savedMapping.setOriginalUrl("https://secure.example.com/path?param=value");
        savedMapping.setCreatedAt(LocalDateTime.now());

        when(repository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(idGenerationServiceImpl.generateUniqueShortCode()).thenReturn("abc123");
        when(repository.save(any(UrlMapping.class))).thenReturn(savedMapping);

        // Act & Assert - Should not throw exception
        CreateUrlResponse response = urlService.createShortUrl(request);
        
        assertNotNull(response);
        assertEquals("https://secure.example.com/path?param=value", response.getOriginalUrl());
    }

    @Test
    void testCacheIntegration_FullFlow() {
        // Arrange
        String shortCode = "abc123";
        UrlMapping dbMapping = new UrlMapping();
        dbMapping.setShortCode(shortCode);
        dbMapping.setOriginalUrl("https://www.example.com");
        dbMapping.setCreatedAt(LocalDateTime.now());

        // First call - cache miss
        when(cacheService.getUrlMapping(shortCode, UrlMapping.class))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(dbMapping)); // Second call - cache hit

        when(repository.findByShortCode(shortCode)).thenReturn(Optional.of(dbMapping));

        // Act - First call (cache miss)
        String originalUrl1 = urlService.getOriginalUrl(shortCode);

        // Act - Second call (should be cache hit)
        String originalUrl2 = urlService.getOriginalUrl(shortCode);

        // Assert
        assertEquals("https://www.example.com", originalUrl1);
        assertEquals("https://www.example.com", originalUrl2);

        // Verify cache behavior
        verify(cacheService, times(2)).getUrlMapping(shortCode, UrlMapping.class);
        verify(repository, times(1)).findByShortCode(shortCode); // Only called once
        verify(cacheService, times(1)).putUrlMapping(shortCode, dbMapping, cacheTtl);
    }

    // ========== Task 5.1 Tests: User Ownership Functionality ==========

    private User createTestUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPasswordHash("hashedPassword");
        return user;
    }

    @Test
    void testCreateShortUrl_WithUser_Success() {
        // Arrange
        User owner = createTestUser(1L, "test@example.com");
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        UrlMapping savedMapping = new UrlMapping();
        savedMapping.setId(123L);
        savedMapping.setShortCode("abc123");
        savedMapping.setOriginalUrl("https://www.example.com");
        savedMapping.setOwner(owner);
        savedMapping.setCreatedAt(LocalDateTime.now());

        when(repository.findByOriginalUrlAndOwner("https://www.example.com", owner)).thenReturn(Optional.empty());
        when(idGenerationServiceImpl.generateUniqueShortCode()).thenReturn("abc123");
        when(repository.save(any(UrlMapping.class))).thenReturn(savedMapping);

        // Act
        CreateUrlResponse response = urlService.createShortUrl(request, owner);

        // Assert
        assertNotNull(response);
        assertEquals("abc123", response.getShortCode());
        assertEquals("https://www.example.com", response.getOriginalUrl());

        verify(repository).findByOriginalUrlAndOwner("https://www.example.com", owner);
        verify(repository).save(any(UrlMapping.class));
        verify(cacheService).putUrlMapping(eq("abc123"), any(UrlMapping.class), eq(cacheTtl));
    }

    @Test
    void testCreateShortUrl_WithUser_DuplicateUrl() {
        // Arrange
        User owner = createTestUser(1L, "test@example.com");
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        UrlMapping existingMapping = new UrlMapping();
        existingMapping.setId(456L);
        existingMapping.setShortCode("existing123");
        existingMapping.setOriginalUrl("https://www.example.com");
        existingMapping.setOwner(owner);
        existingMapping.setCreatedAt(LocalDateTime.now());

        when(repository.findByOriginalUrlAndOwner("https://www.example.com", owner))
                .thenReturn(Optional.of(existingMapping));

        // Act
        CreateUrlResponse response = urlService.createShortUrl(request, owner);

        // Assert
        assertNotNull(response);
        assertEquals("existing123", response.getShortCode());
        assertEquals("https://www.example.com", response.getOriginalUrl());

        verify(repository).findByOriginalUrlAndOwner("https://www.example.com", owner);
        verify(repository, never()).save(any(UrlMapping.class));
        verify(idGenerationServiceImpl, never()).generateUniqueShortCode();
        verify(cacheService).putUrlMapping("existing123", existingMapping, cacheTtl);
    }

    @Test
    void testCreateShortUrl_WithUser_ExpirationDate() {
        // Arrange
        User owner = createTestUser(1L, "test@example.com");
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");
        LocalDateTime expirationDate = LocalDateTime.now().plusDays(7);
        request.setExpiresAt(expirationDate);

        UrlMapping savedMapping = new UrlMapping();
        savedMapping.setId(123L);
        savedMapping.setShortCode("abc123");
        savedMapping.setOriginalUrl("https://www.example.com");
        savedMapping.setOwner(owner);
        savedMapping.setExpiresAt(expirationDate);
        savedMapping.setCreatedAt(LocalDateTime.now());

        when(repository.findByOriginalUrlAndOwner("https://www.example.com", owner)).thenReturn(Optional.empty());
        when(idGenerationServiceImpl.generateUniqueShortCode()).thenReturn("abc123");
        when(repository.save(any(UrlMapping.class))).thenReturn(savedMapping);

        // Act
        CreateUrlResponse response = urlService.createShortUrl(request, owner);

        // Assert
        assertNotNull(response);
        assertEquals("abc123", response.getShortCode());
        verify(repository).save(argThat(mapping -> 
            mapping.getExpiresAt() != null && mapping.getExpiresAt().equals(expirationDate)
        ));
    }

    @Test
    void testGetUserUrls_Success() {
        // Arrange
        User owner = createTestUser(1L, "test@example.com");
        Pageable pageable = PageRequest.of(0, 10);

        UrlMapping mapping1 = new UrlMapping();
        mapping1.setShortCode("abc123");
        mapping1.setOriginalUrl("https://www.example1.com");
        mapping1.setOwner(owner);
        mapping1.setCreatedAt(LocalDateTime.now());

        UrlMapping mapping2 = new UrlMapping();
        mapping2.setShortCode("def456");
        mapping2.setOriginalUrl("https://www.example2.com");
        mapping2.setOwner(owner);
        mapping2.setCreatedAt(LocalDateTime.now());

        List<UrlMapping> mappings = Arrays.asList(mapping1, mapping2);
        Page<UrlMapping> page = new PageImpl<>(mappings, pageable, 2);

        when(repository.findByOwner(owner, pageable)).thenReturn(page);

        // Act
        Page<UrlMappingResponse> result = urlService.getUserUrls(owner, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals("abc123", result.getContent().get(0).getShortCode());
        assertEquals("def456", result.getContent().get(1).getShortCode());

        verify(repository).findByOwner(owner, pageable);
    }

    @Test
    void testGetUserUrls_NullOwner() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            urlService.getUserUrls(null, pageable);
        });

        assertEquals("Owner cannot be null", exception.getMessage());
        verify(repository, never()).findByOwner(any(), any());
    }

    @Test
    void testGetUserActiveUrls_Success() {
        // Arrange
        User owner = createTestUser(1L, "test@example.com");
        Pageable pageable = PageRequest.of(0, 10);

        UrlMapping activeMapping = new UrlMapping();
        activeMapping.setShortCode("abc123");
        activeMapping.setOriginalUrl("https://www.example.com");
        activeMapping.setOwner(owner);
        activeMapping.setIsActive(true);
        activeMapping.setCreatedAt(LocalDateTime.now());

        List<UrlMapping> mappings = Arrays.asList(activeMapping);
        Page<UrlMapping> page = new PageImpl<>(mappings, pageable, 1);

        when(repository.findByOwnerAndIsActive(owner, true, pageable)).thenReturn(page);

        // Act
        Page<UrlMappingResponse> result = urlService.getUserActiveUrls(owner, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("abc123", result.getContent().get(0).getShortCode());

        verify(repository).findByOwnerAndIsActive(owner, true, pageable);
    }

    @Test
    void testGetUserActiveUrls_NullOwner() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            urlService.getUserActiveUrls(null, pageable);
        });

        assertEquals("Owner cannot be null", exception.getMessage());
        verify(repository, never()).findByOwnerAndIsActive(any(), any(), any());
    }

    @Test
    void testIsUrlOwnedByUser_True() {
        // Arrange
        User owner = createTestUser(1L, "test@example.com");
        String shortCode = "abc123";

        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode(shortCode);
        mapping.setOwner(owner);

        when(repository.findByShortCodeAndOwner(shortCode, owner)).thenReturn(Optional.of(mapping));

        // Act
        boolean result = urlService.isUrlOwnedByUser(shortCode, owner);

        // Assert
        assertTrue(result);
        verify(repository).findByShortCodeAndOwner(shortCode, owner);
    }

    @Test
    void testIsUrlOwnedByUser_False() {
        // Arrange
        User owner = createTestUser(1L, "test@example.com");
        String shortCode = "abc123";

        when(repository.findByShortCodeAndOwner(shortCode, owner)).thenReturn(Optional.empty());

        // Act
        boolean result = urlService.isUrlOwnedByUser(shortCode, owner);

        // Assert
        assertFalse(result);
        verify(repository).findByShortCodeAndOwner(shortCode, owner);
    }

    @Test
    void testIsUrlOwnedByUser_NullUser() {
        // Arrange
        String shortCode = "abc123";

        // Act
        boolean result = urlService.isUrlOwnedByUser(shortCode, null);

        // Assert
        assertFalse(result);
        verify(repository, never()).findByShortCodeAndOwner(any(), any());
    }

    @Test
    void testIsUrlOwnedByUser_NullShortCode() {
        // Arrange
        User owner = createTestUser(1L, "test@example.com");

        // Act
        boolean result = urlService.isUrlOwnedByUser(null, owner);

        // Assert
        assertFalse(result);
        verify(repository, never()).findByShortCodeAndOwner(any(), any());
    }

    @Test
    void testValidateUrlOwnership_Success() {
        // Arrange
        User owner = createTestUser(1L, "test@example.com");
        String shortCode = "abc123";

        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode(shortCode);
        mapping.setOwner(owner);

        when(repository.findByShortCodeAndOwner(shortCode, owner)).thenReturn(Optional.of(mapping));

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> {
            urlService.validateUrlOwnership(shortCode, owner);
        });

        verify(repository).findByShortCodeAndOwner(shortCode, owner);
    }

    @Test
    void testValidateUrlOwnership_NullUser() {
        // Arrange
        String shortCode = "abc123";

        // Act & Assert
        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class, () -> {
            urlService.validateUrlOwnership(shortCode, null);
        });

        assertEquals("User Authentication Required", exception.getMessage());
        verify(repository, never()).findByShortCodeAndOwner(any(), any());
    }

    @Test
    void testValidateUrlOwnership_NotOwned() {
        // Arrange
        User owner = createTestUser(1L, "test@example.com");
        String shortCode = "abc123";

        when(repository.findByShortCodeAndOwner(shortCode, owner)).thenReturn(Optional.empty());

        // Act & Assert
        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class, () -> {
            urlService.validateUrlOwnership(shortCode, owner);
        });

        assertEquals("You don't have permission to access this URL", exception.getMessage());
        verify(repository).findByShortCodeAndOwner(shortCode, owner);
    }

    @Test
    void testGetUserUrlCount_Success() {
        // Arrange
        User owner = createTestUser(1L, "test@example.com");
        long expectedCount = 5L;

        when(repository.countByOwner(owner)).thenReturn(expectedCount);

        // Act
        long result = urlService.getUserUrlCount(owner);

        // Assert
        assertEquals(expectedCount, result);
        verify(repository).countByOwner(owner);
    }

    @Test
    void testGetUserUrlCount_NullOwner() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            urlService.getUserUrlCount(null);
        });

        assertEquals("Owner cannot be null", exception.getMessage());
        verify(repository, never()).countByOwner(any());
    }

    @Test
    void testGetUserActiveUrlCount_Success() {
        // Arrange
        User owner = createTestUser(1L, "test@example.com");
        long expectedCount = 3L;

        when(repository.countByOwnerAndIsActive(owner, true)).thenReturn(expectedCount);

        // Act
        long result = urlService.getUserActiveUrlCount(owner);

        // Assert
        assertEquals(expectedCount, result);
        verify(repository).countByOwnerAndIsActive(owner, true);
    }

    @Test
    void testGetUserActiveUrlCount_NullOwner() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            urlService.getUserActiveUrlCount(null);
        });

        assertEquals("Owner cannot be null", exception.getMessage());
        verify(repository, never()).countByOwnerAndIsActive(any(), any());
    }

    @Test
    void testBackwardCompatibility_AnonymousUrlCreation() {
        // Arrange
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        UrlMapping savedMapping = new UrlMapping();
        savedMapping.setId(123L);
        savedMapping.setShortCode("abc123");
        savedMapping.setOriginalUrl("https://www.example.com");
        savedMapping.setOwner(null); // Anonymous URL
        savedMapping.setCreatedAt(LocalDateTime.now());

        when(repository.findByOriginalUrl("https://www.example.com")).thenReturn(Optional.empty());
        when(idGenerationServiceImpl.generateUniqueShortCode()).thenReturn("abc123");
        when(repository.save(any(UrlMapping.class))).thenReturn(savedMapping);

        // Act
        CreateUrlResponse response = urlService.createShortUrl(request);

        // Assert
        assertNotNull(response);
        assertEquals("abc123", response.getShortCode());

        // Verify that user-specific duplicate detection was not called
        verify(repository, never()).findByOriginalUrlAndOwner(any(), any());
        verify(repository).findByOriginalUrl("https://www.example.com");
        verify(repository).save(argThat(mapping -> mapping.getOwner() == null));
    }

    @Test
    void testUserIsolation_DifferentUsersCanHaveSameUrl() {
        // Arrange
        User user1 = createTestUser(1L, "user1@example.com");
        User user2 = createTestUser(2L, "user2@example.com");
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        // Mock for user1 - no existing URL
        when(repository.findByOriginalUrlAndOwner("https://www.example.com", user1))
                .thenReturn(Optional.empty());
        
        // Mock for user2 - no existing URL
        when(repository.findByOriginalUrlAndOwner("https://www.example.com", user2))
                .thenReturn(Optional.empty());

        UrlMapping savedMapping1 = new UrlMapping();
        savedMapping1.setShortCode("abc123");
        savedMapping1.setOriginalUrl("https://www.example.com");
        savedMapping1.setOwner(user1);

        UrlMapping savedMapping2 = new UrlMapping();
        savedMapping2.setShortCode("def456");
        savedMapping2.setOriginalUrl("https://www.example.com");
        savedMapping2.setOwner(user2);

        when(idGenerationServiceImpl.generateUniqueShortCode())
                .thenReturn("abc123")
                .thenReturn("def456");
        
        when(repository.save(any(UrlMapping.class)))
                .thenReturn(savedMapping1)
                .thenReturn(savedMapping2);

        // Act
        CreateUrlResponse response1 = urlService.createShortUrl(request, user1);
        CreateUrlResponse response2 = urlService.createShortUrl(request, user2);

        // Assert
        assertNotNull(response1);
        assertNotNull(response2);
        assertEquals("abc123", response1.getShortCode());
        assertEquals("def456", response2.getShortCode());
        
        // Both users should be able to create URLs for the same original URL
        verify(repository).findByOriginalUrlAndOwner("https://www.example.com", user1);
        verify(repository).findByOriginalUrlAndOwner("https://www.example.com", user2);
        verify(repository, times(2)).save(any(UrlMapping.class));
    }
}