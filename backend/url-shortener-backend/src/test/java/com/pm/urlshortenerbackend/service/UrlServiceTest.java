package com.pm.urlshortenerbackend.service;

import com.pm.urlshortenerbackend.dto.CreateUrlRequest;
import com.pm.urlshortenerbackend.dto.CreateUrlResponse;
import com.pm.urlshortenerbackend.dto.UrlMappingResponse;
import com.pm.urlshortenerbackend.exception.InvalidUrlException;
import com.pm.urlshortenerbackend.exception.UrlNotFoundException;
import com.pm.urlshortenerbackend.model.UrlMapping;
import com.pm.urlshortenerbackend.repository.UrlMappingRepository;
import com.pm.urlshortenerbackend.service.impl.IdGenerationServiceImpl;
import com.pm.urlshortenerbackend.service.impl.UrlServiceImpl;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
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
        savedMapping.setShortCode("abc123");  // shortCode set from the start
        savedMapping.setOriginalUrl("https://www.example.com");
        savedMapping.setCreatedAt(LocalDateTime.now());

        when(repository.findByOriginalUrl("https://www.example.com")).thenReturn(Optional.empty());
        when(idGenerationServiceImpl.generateUniqueShortCode()).thenReturn("abc123");  // Updated method
        when(repository.save(any(UrlMapping.class))).thenReturn(savedMapping);

        // Act
        CreateUrlResponse response = urlService.createShortUrl(request);

        // Assert
        assertNotNull(response);
        assertEquals("abc123", response.getShortCode());

        verify(repository, times(1)).save(any(UrlMapping.class));  // Only one save now
        verify(idGenerationServiceImpl).generateUniqueShortCode();  // Updated method call
        verify(cacheService).putUrlMapping("abc123", savedMapping, cacheTtl);
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
        assertEquals("http://localhost:8080/existing123", response.getShortUrl());
        assertEquals("https://www.example.com", response.getOriginalUrl());

        verify(repository, never()).save(any(UrlMapping.class));
        verify(idGenerationServiceImpl, never()).generateUniqueId(anyLong());
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
        savedMapping.setOriginalUrl("https://www.example.com");
        savedMapping.setCreatedAt(LocalDateTime.now());

        UrlMapping finalMapping = new UrlMapping();
        finalMapping.setId(123L);
        finalMapping.setShortCode("abc123");
        finalMapping.setOriginalUrl("https://www.example.com");
        finalMapping.setCreatedAt(savedMapping.getCreatedAt());

        when(repository.save(any(UrlMapping.class))).thenReturn(savedMapping).thenReturn(finalMapping);
        when(idGenerationServiceImpl.generateUniqueId(123L)).thenReturn("abc123");

        // Act
        CreateUrlResponse response = urlService.createShortUrl(request);

        // Assert
        assertNotNull(response);
        assertEquals("abc123", response.getShortCode());
        
        // Verify duplicate detection was skipped
        verify(repository, never()).findByOriginalUrl(anyString());
        verify(repository, times(2)).save(any(UrlMapping.class));
    }

    @Test
    void testValidUrl_HttpsProtocol() {
        // Arrange
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://secure.example.com/path?param=value");

        UrlMapping savedMapping = new UrlMapping();
        savedMapping.setId(123L);
        savedMapping.setCreatedAt(LocalDateTime.now());

        UrlMapping finalMapping = new UrlMapping();
        finalMapping.setId(123L);
        finalMapping.setShortCode("abc123");
        finalMapping.setOriginalUrl("https://secure.example.com/path?param=value");
        finalMapping.setCreatedAt(savedMapping.getCreatedAt());

        when(repository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(UrlMapping.class))).thenReturn(savedMapping).thenReturn(finalMapping);
        when(idGenerationServiceImpl.generateUniqueId(123L)).thenReturn("abc123");

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
}