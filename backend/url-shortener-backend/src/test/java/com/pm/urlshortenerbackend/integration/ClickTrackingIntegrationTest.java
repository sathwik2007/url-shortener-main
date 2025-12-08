package com.pm.urlshortenerbackend.integration;

import com.pm.urlshortenerbackend.dto.CreateUrlRequest;
import com.pm.urlshortenerbackend.dto.CreateUrlResponse;
import com.pm.urlshortenerbackend.model.ClickEvent;
import com.pm.urlshortenerbackend.model.UrlMapping;
import com.pm.urlshortenerbackend.repository.ClickEventRepository;
import com.pm.urlshortenerbackend.repository.UrlMappingRepository;
import com.pm.urlshortenerbackend.service.UrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Author: Sathwik Pillalamarri
 * Date: 12/3/25
 * Project: url-shortener-backend
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
public class ClickTrackingIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @Autowired
    private ClickEventRepository clickEventRepository;

    @Autowired
    private UrlService urlService;

    @Test
    void testRedirectLogsClick() throws Exception {
        // Create a URL mapping
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");
        CreateUrlResponse response = urlService.createShortUrl(request);

        String shortCode = response.getShortCode();

        // Make redirect request
        ResponseEntity<Void> redirectResponse = restTemplate.getForEntity(
                "/" + shortCode,
                Void.class
        );

        // Verify redirect
        assertEquals(HttpStatus.FOUND, redirectResponse.getStatusCode());
        assertEquals("https://www.example.com",
                redirectResponse.getHeaders().getLocation().toString());

        // Wait for async click tracking
        Thread.sleep(1000);

        // Verify click was logged
        UrlMapping urlMapping = urlMappingRepository.findByShortCode(shortCode).orElseThrow();
        List<ClickEvent> clicks = clickEventRepository.findByUrlMapping(urlMapping);

        assertEquals(1, clicks.size());
        assertNotNull(clicks.get(0).getIpAddressHash());
        assertNotNull(clicks.get(0).getClickedAt());
    }

    @Test
    void testRedirectWorksEvenIfClickTrackingFails() {
        // This test ensures redirect still works if click tracking has issues
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");
        CreateUrlResponse response = urlService.createShortUrl(request);

        String shortCode = response.getShortCode();

        // Make redirect request
        ResponseEntity<Void> redirectResponse = restTemplate.getForEntity(
                "/" + shortCode,
                Void.class
        );

        // Verify redirect works regardless of click tracking
        assertEquals(HttpStatus.FOUND, redirectResponse.getStatusCode());
    }

    @Test
    void testExpiredUrlReturns410() {
        // Create expired URL
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");
        request.setExpiresAt(LocalDateTime.now().minusDays(1));
        CreateUrlResponse response = urlService.createShortUrl(request);

        String shortCode = response.getShortCode();

        // Make redirect request
        ResponseEntity<Void> redirectResponse = restTemplate.getForEntity(
                "/" + shortCode,
                Void.class
        );

        // Verify 410 Gone
        assertEquals(HttpStatus.GONE, redirectResponse.getStatusCode());
    }
}
