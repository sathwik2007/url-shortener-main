package com.pm.urlshortenerbackend.health;

import com.pm.urlshortenerbackend.dto.CreateUrlRequest;
import com.pm.urlshortenerbackend.dto.CreateUrlResponse;
import com.pm.urlshortenerbackend.service.ClickTrackingService;
import com.pm.urlshortenerbackend.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Author: Sathwik Pillalamarri
 * Date: 12/3/25
 * Project: url-shortener-backend
 */
@SpringBootTest
public class RedirectPerformanceTest {
    @Autowired
    private UrlService urlService;

    @Autowired
    private ClickTrackingService clickTrackingService;

    @Autowired
    private HttpServletRequest request;

    @Test
    void testRedirectPerformanceNotImpactedByClickTracking() {
        // Create URL
        CreateUrlRequest createRequest = new CreateUrlRequest();
        createRequest.setOriginalUrl("https://www.example.com");
        CreateUrlResponse response = urlService.createShortUrl(createRequest);

        String shortCode = response.getShortCode();

        // Measure redirect time without click tracking
        long startWithout = System.nanoTime();
        String url1 = urlService.getOriginalUrl(shortCode);
        long timeWithout = System.nanoTime() - startWithout;

        // Measure redirect time with click tracking
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test Agent");
        when(request.getHeader("Referer")).thenReturn(null);

        long startWith = System.nanoTime();
        String url2 = urlService.getOriginalUrl(shortCode);
        // Create ClickEventData from request
        com.pm.urlshortenerbackend.dto.ClickEventData clickEventData = 
            new com.pm.urlshortenerbackend.dto.ClickEventData("127.0.0.1", "Test Agent", null);
        clickTrackingService.logClick(shortCode, clickEventData);
        long timeWith = System.nanoTime() - startWith;

        // Verify redirect time is not significantly impacted
        // Click tracking should be async and not add more than 5ms
        assertTrue(timeWith - timeWithout < 5_000_000, // 5ms in nanoseconds
                "Click tracking should not significantly impact redirect time");
    }
}
