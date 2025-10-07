package com.pm.urlshortenerbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.urlshortenerbackend.dto.CreateUrlRequest;
import com.pm.urlshortenerbackend.dto.CreateUrlResponse;
import com.pm.urlshortenerbackend.exception.InvalidUrlException;
import com.pm.urlshortenerbackend.exception.UrlNotFoundException;
import com.pm.urlshortenerbackend.service.UrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for UrlController
 * Tests Task 6.1 requirements: POST /api/links endpoint with validation and error handling
 */
@WebMvcTest(UrlController.class)
public class UrlControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UrlService urlService;

    @Autowired
    private ObjectMapper objectMapper;

    // ========== Task 6.1 Tests: POST /api/links Endpoint ==========

    @Test
    void testCreateShortUrl_Success() throws Exception {
        // Arrange
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        CreateUrlResponse response = new CreateUrlResponse(
                "abc123",
                "http://localhost:8080/abc123",
                "https://www.example.com",
                LocalDateTime.now()
        );

        when(urlService.createShortUrl(any(CreateUrlRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.shortCode").value("abc123"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/abc123"))
                .andExpect(jsonPath("$.originalUrl").value("https://www.example.com"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void testCreateShortUrl_InvalidUrl_BadRequest() throws Exception {
        // Arrange
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("invalid-url");

        when(urlService.createShortUrl(any(CreateUrlRequest.class)))
                .thenThrow(new InvalidUrlException("Malformed URL: invalid-url"));

        // Act & Assert
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Malformed URL: invalid-url"));
    }

    @Test
    void testCreateShortUrl_ValidationErrors_EmptyUrl() throws Exception {
        // Arrange
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl(""); // Empty URL should fail validation

        // Act & Assert
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.originalUrl").exists()); // Should contain validation error for originalUrl field
    }

    @Test
    void testCreateShortUrl_ValidationErrors_NullUrl() throws Exception {
        // Arrange
        CreateUrlRequest request = new CreateUrlRequest();
        // originalUrl is null by default

        // Act & Assert
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.originalUrl").exists()); // Should contain validation error
    }

    @Test
    void testCreateShortUrl_ValidationErrors_InvalidProtocol() throws Exception {
        // Arrange
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("ftp://example.com"); // Invalid protocol should fail validation

        // Act & Assert
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.originalUrl").exists()); // Should contain validation error
    }

    @Test
    void testCreateShortUrl_ValidationErrors_TooLongUrl() throws Exception {
        // Arrange
        CreateUrlRequest request = new CreateUrlRequest();
        String longUrl = "https://www.example.com/" + "a".repeat(2048); // Exceeds max length
        request.setOriginalUrl(longUrl);

        // Act & Assert
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.originalUrl").exists()); // Should contain validation error
    }

    @Test
    void testCreateShortUrl_InvalidJson() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("invalid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateShortUrl_MissingContentType() throws Exception {
        // Arrange
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        // Act & Assert
        mockMvc.perform(post("/api/links")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void testCreateShortUrl_InternalServerError() throws Exception {
        // Arrange
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        when(urlService.createShortUrl(any(CreateUrlRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }

    @Test
    void testCreateShortUrl_DuplicateUrl() throws Exception {
        // Arrange
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        CreateUrlResponse response = new CreateUrlResponse(
                "existing123",
                "http://localhost:8080/existing123",
                "https://www.example.com",
                LocalDateTime.now().minusHours(1) // Created earlier
        );

        when(urlService.createShortUrl(any(CreateUrlRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()) // Should still return 201 for existing URLs
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.shortCode").value("existing123"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/existing123"))
                .andExpect(jsonPath("$.originalUrl").value("https://www.example.com"));
    }

    @Test
    void testCreateShortUrl_HttpsUrl() throws Exception {
        // Arrange
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://secure.example.com/path?param=value");

        CreateUrlResponse response = new CreateUrlResponse(
                "secure123",
                "http://localhost:8080/secure123",
                "https://secure.example.com/path?param=value",
                LocalDateTime.now()
        );

        when(urlService.createShortUrl(any(CreateUrlRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.shortCode").value("secure123"))
                .andExpect(jsonPath("$.originalUrl").value("https://secure.example.com/path?param=value"));
    }

    // ========== Error Handling Tests ==========

    @Test
    void testGlobalExceptionHandler_InvalidUrlException() throws Exception {
        // Arrange
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        when(urlService.createShortUrl(any(CreateUrlRequest.class)))
                .thenThrow(new InvalidUrlException("Invalid Protocol: ftp"));

        // Act & Assert
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Invalid Protocol: ftp"));
    }

    @Test
    void testGlobalExceptionHandler_ValidationErrors() throws Exception {
        // Arrange - Create request with multiple validation errors
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl(""); // Empty URL

        // Act & Assert
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.originalUrl").exists()); // Field-level error
    }

    // ========== Content Type and Format Tests ==========

    @Test
    void testCreateShortUrl_CorrectContentType() throws Exception {
        // Arrange
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        CreateUrlResponse response = new CreateUrlResponse(
                "abc123",
                "http://localhost:8080/abc123",
                "https://www.example.com",
                LocalDateTime.now()
        );

        when(urlService.createShortUrl(any(CreateUrlRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testCreateShortUrl_ResponseStructure() throws Exception {
        // Arrange
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        CreateUrlResponse response = new CreateUrlResponse(
                "abc123",
                "http://localhost:8080/abc123",
                "https://www.example.com",
                LocalDateTime.now()
        );

        when(urlService.createShortUrl(any(CreateUrlRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").isString())
                .andExpect(jsonPath("$.shortUrl").isString())
                .andExpect(jsonPath("$.originalUrl").isString())
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.shortCode").isNotEmpty())
                .andExpect(jsonPath("$.shortUrl").isNotEmpty())
                .andExpect(jsonPath("$.originalUrl").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    // ========== Task 6.2 Tests: GET /{shortCode} Redirect Endpoint ==========

    @Test
    void testRedirectToOriginalUrl_Success() throws Exception {
        // Arrange
        String shortCode = "abc123";
        String originalUrl = "https://www.example.com";

        when(urlService.getOriginalUrl(shortCode)).thenReturn(originalUrl);

        // Act & Assert
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", originalUrl))
                .andExpect(content().string(""));
    }

    @Test
    void testRedirectToOriginalUrl_HttpsUrl() throws Exception {
        // Arrange
        String shortCode = "secure123";
        String originalUrl = "https://secure.example.com/path?param=value&another=test";

        when(urlService.getOriginalUrl(shortCode)).thenReturn(originalUrl);

        // Act & Assert
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", originalUrl))
                .andExpect(content().string(""));
    }

    @Test
    void testRedirectToOriginalUrl_ComplexUrl() throws Exception {
        // Arrange
        String shortCode = "complex1";
        String originalUrl = "https://www.example.com/path/to/resource?query=test&filter=active#section";

        when(urlService.getOriginalUrl(shortCode)).thenReturn(originalUrl);

        // Act & Assert
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", originalUrl))
                .andExpect(content().string(""));
    }

    @Test
    void testRedirectToOriginalUrl_ShortCodeNotFound() throws Exception {
        // Arrange
        String shortCode = "notfound";

        when(urlService.getOriginalUrl(shortCode))
                .thenThrow(new UrlNotFoundException("Short code not found: " + shortCode));

        // Act & Assert
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Short code not found: " + shortCode));
    }

    @Test
    void testRedirectToOriginalUrl_InvalidShortCodeFormat() throws Exception {
        // Arrange - Test various invalid short code formats
        String[] invalidShortCodes = {
                "abc@123",      // Contains special character
                "abc 123",      // Contains space
                "abc-123",      // Contains hyphen
                "abc_123",      // Contains underscore
                "verylongshortcode123", // Too long (>10 chars)
                "",             // Empty string
                "abc#123"       // Contains hash
        };

        for (String invalidShortCode : invalidShortCodes) {
            // Act & Assert
            mockMvc.perform(get("/" + invalidShortCode))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.originalUrl").exists()); // Validation error for shortCode
        }
    }

    @Test
    void testRedirectToOriginalUrl_ValidShortCodeFormats() throws Exception {
        // Arrange - Test various valid short code formats
        String[] validShortCodes = {
                "a",            // Single character
                "A",            // Single uppercase
                "1",            // Single digit
                "abc123",       // Mixed alphanumeric
                "ABC123",       // Uppercase alphanumeric
                "1234567890",   // All digits (10 chars)
                "abcdefghij"    // All letters (10 chars)
        };

        for (String validShortCode : validShortCodes) {
            String originalUrl = "https://www.example.com/" + validShortCode;
            when(urlService.getOriginalUrl(validShortCode)).thenReturn(originalUrl);

            // Act & Assert
            mockMvc.perform(get("/" + validShortCode))
                    .andExpect(status().isMovedPermanently())
                    .andExpect(header().string("Location", originalUrl));
        }
    }

    @Test
    void testRedirectToOriginalUrl_CaseInsensitive() throws Exception {
        // Arrange
        String shortCode = "AbC123";
        String originalUrl = "https://www.example.com";

        when(urlService.getOriginalUrl(shortCode)).thenReturn(originalUrl);

        // Act & Assert
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", originalUrl));
    }

    @Test
    void testRedirectToOriginalUrl_ServiceException() throws Exception {
        // Arrange
        String shortCode = "error123";

        when(urlService.getOriginalUrl(shortCode))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }

    @Test
    void testRedirectToOriginalUrl_HttpStatusCode() throws Exception {
        // Arrange
        String shortCode = "status123";
        String originalUrl = "https://www.example.com";

        when(urlService.getOriginalUrl(shortCode)).thenReturn(originalUrl);

        // Act & Assert
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().is(301)) // Explicitly test for 301 status
                .andExpect(header().string("Location", originalUrl));
    }

    @Test
    void testRedirectToOriginalUrl_NoResponseBody() throws Exception {
        // Arrange
        String shortCode = "nobody123";
        String originalUrl = "https://www.example.com";

        when(urlService.getOriginalUrl(shortCode)).thenReturn(originalUrl);

        // Act & Assert
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", originalUrl))
                .andExpect(content().string("")) // Should have empty body
                .andExpect(header().doesNotExist("Content-Type")); // No content type for empty body
    }

    @Test
    void testRedirectToOriginalUrl_LocationHeaderPresent() throws Exception {
        // Arrange
        String shortCode = "location1";
        String originalUrl = "https://www.example.com/test";

        when(urlService.getOriginalUrl(shortCode)).thenReturn(originalUrl);

        // Act & Assert
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", originalUrl));
    }

    @Test
    void testRedirectToOriginalUrl_UrlWithSpecialCharacters() throws Exception {
        // Arrange
        String shortCode = "special1";
        String originalUrl = "https://www.example.com/path?query=hello%20world&param=test+value";

        when(urlService.getOriginalUrl(shortCode)).thenReturn(originalUrl);

        // Act & Assert
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", originalUrl));
    }

    // ========== HTTP Protocol Compliance Tests for Task 6.2 ==========

    @Test
    void testRedirectToOriginalUrl_HttpProtocolCompliance() throws Exception {
        // Arrange
        String shortCode = "protocol1";
        String originalUrl = "https://www.example.com";

        when(urlService.getOriginalUrl(shortCode)).thenReturn(originalUrl);

        // Act & Assert - Test HTTP/1.1 compliance for 301 redirects
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isMovedPermanently()) // 301 status
                .andExpect(header().exists("Location"))   // Location header required
                .andExpect(header().string("Location", originalUrl))
                .andExpect(content().string(""));         // Empty body for 301
    }

    @Test
    void testRedirectToOriginalUrl_MultipleRequests() throws Exception {
        // Arrange - Test that multiple requests work correctly
        String shortCode = "multi123";
        String originalUrl = "https://www.example.com";

        when(urlService.getOriginalUrl(shortCode)).thenReturn(originalUrl);

        // Act & Assert - Make multiple requests
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/" + shortCode))
                    .andExpect(status().isMovedPermanently())
                    .andExpect(header().string("Location", originalUrl));
        }
    }
}