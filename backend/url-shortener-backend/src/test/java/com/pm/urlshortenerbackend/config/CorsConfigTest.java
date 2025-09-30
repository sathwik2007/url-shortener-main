package com.pm.urlshortenerbackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.urlshortenerbackend.dto.CreateUrlRequest;
import com.pm.urlshortenerbackend.dto.CreateUrlResponse;
import com.pm.urlshortenerbackend.service.UrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for CORS configuration
 * Tests Task 7.2 requirements: CORS configuration for Next.js frontend
 * 
 * Author: Sathwik Pillalamarri
 * Date: 9/30/25
 * Project: url-shortener-backend
 */
@WebMvcTest
@TestPropertySource(properties = {
    "app.cors.allowed-origins=http://localhost:3000,http://localhost:3001",
    "app.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS",
    "app.cors.allowed-headers=*",
    "app.cors.allow-credentials=true",
    "app.cors.max-age=3600"
})
public class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UrlService urlService;

    @Autowired
    private ObjectMapper objectMapper;

    // ========== CORS Preflight Request Tests ==========

    @Test
    void testCorsPreflightRequest_AllowedOrigin() throws Exception {
        // Test OPTIONS preflight request from allowed origin
        mockMvc.perform(options("/api/links")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Headers", "*"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Max-Age", "3600"));
    }

    @Test
    void testCorsPreflightRequest_SecondAllowedOrigin() throws Exception {
        // Test OPTIONS preflight request from second allowed origin (localhost:3001)
        mockMvc.perform(options("/api/links")
                .header("Origin", "http://localhost:3001")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3001"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS"));
    }

    // ========== CORS Actual Request Tests ==========

    @Test
    void testCorsActualRequest_POST_CreateUrl() throws Exception {
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

        // Test actual POST request with CORS headers
        mockMvc.perform(post("/api/links")
                .header("Origin", "http://localhost:3000")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(jsonPath("$.shortCode").value("abc123"))
                .andExpect(jsonPath("$.originalUrl").value("https://www.example.com"));
    }

    @Test
    void testCorsActualRequest_GET_Redirect() throws Exception {
        // Arrange
        when(urlService.getOriginalUrl("abc123")).thenReturn("https://www.example.com");

        // Test actual GET request with CORS headers
        mockMvc.perform(get("/abc123")
                .header("Origin", "http://localhost:3000"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Location", "https://www.example.com"));
    }

    // ========== CORS Security Tests ==========

    @Test
    void testCorsRejectedOrigin() throws Exception {
        // Test request from non-allowed origin should be rejected
        mockMvc.perform(options("/api/links")
                .header("Origin", "http://malicious-site.com")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCorsNoOriginHeader() throws Exception {
        // Test request without Origin header (same-origin request)
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        CreateUrlResponse response = new CreateUrlResponse(
                "abc123",
                "http://localhost:8080/abc123",
                "https://www.example.com",
                LocalDateTime.now()
        );

        when(urlService.createShortUrl(any(CreateUrlRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("abc123"));
    }

    // ========== CORS Credentials Tests ==========

    @Test
    void testCorsWithCredentials() throws Exception {
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

        // Test request with credentials (cookies, authorization headers)
        mockMvc.perform(post("/api/links")
                .header("Origin", "http://localhost:3000")
                .header("Cookie", "sessionId=abc123")
                .header("Authorization", "Bearer token123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(jsonPath("$.shortCode").value("abc123"));
    }

    // ========== CORS Headers Tests ==========

    @Test
    void testCorsCustomHeaders() throws Exception {
        // Test preflight with custom headers
        mockMvc.perform(options("/api/links")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "X-Custom-Header,Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Headers", "*"));
    }

    @Test
    void testCorsAllowedMethods() throws Exception {
        // Test preflight for different HTTP methods
        String[] methods = {"GET", "POST", "PUT", "DELETE", "OPTIONS"};
        
        for (String method : methods) {
            mockMvc.perform(options("/api/links")
                    .header("Origin", "http://localhost:3000")
                    .header("Access-Control-Request-Method", method))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS"));
        }
    }

    // ========== CORS Configuration Validation Tests ==========

    @Test
    void testCorsMaxAge() throws Exception {
        // Test that max-age header is set correctly
        mockMvc.perform(options("/api/links")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Max-Age", "3600"));
    }

    @Test
    void testCorsAllPathsAllowed() throws Exception {
        // Test CORS works for all paths (/**)
        when(urlService.getOriginalUrl("test123")).thenReturn("https://www.example.com");

        mockMvc.perform(get("/test123")
                .header("Origin", "http://localhost:3000"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    // ========== Integration with Frontend Tests ==========

    @Test
    void testNextJsIntegration_CreateAndRedirect() throws Exception {
        // Simulate Next.js frontend workflow: create URL then redirect
        
        // Step 1: Create short URL from Next.js frontend
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com/long-path");

        CreateUrlResponse response = new CreateUrlResponse(
                "nextjs1",
                "http://localhost:8080/nextjs1",
                "https://www.example.com/long-path",
                LocalDateTime.now()
        );

        when(urlService.createShortUrl(any(CreateUrlRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/links")
                .header("Origin", "http://localhost:3000")
                .header("User-Agent", "Mozilla/5.0 (Next.js)")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(jsonPath("$.shortCode").value("nextjs1"));

        // Step 2: Use the short URL for redirect
        when(urlService.getOriginalUrl("nextjs1")).thenReturn("https://www.example.com/long-path");

        mockMvc.perform(get("/nextjs1")
                .header("Origin", "http://localhost:3000"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Location", "https://www.example.com/long-path"));
    }
}