package com.pm.urlshortenerbackend.service;

import com.pm.urlshortenerbackend.dto.ClickStatsResponse;
import com.pm.urlshortenerbackend.model.UrlMapping;
import com.pm.urlshortenerbackend.repository.ClickEventRepository;
import com.pm.urlshortenerbackend.repository.UrlMappingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.Assert.*;

/**
 * Author: Sathwik Pillalamarri
 * Date: 12/9/25
 * Project: url-shortener-backend
 */
@SpringBootTest
public class AnalyticsServiceTest {
    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @Autowired
    private ClickEventRepository clickEventRepository;

    @Test
    void testClickStats() {
        // Create test data
        UrlMapping urlMapping = createTestUrlMapping();
        createTestClickEvents(urlMapping, 10);

        // Get statistics
        ClickStatsResponse stats = analyticsService.getClickStats(urlMapping.getShortCode());

        // Verify
        assertEquals(10, stats.getTotalClicks());
        assertNotNull(stats.getDailyStats());
        assertNotNull(stats.getDeviceStats());
    }

    @Test
    void testCaching() {
        UrlMapping urlMapping = createTestUrlMapping();

        // First call - should calculate
        long start1 = System.currentTimeMillis();
        ClickStatsResponse stats1 = analyticsService.getClickStats(urlMapping.getShortCode());
        long time1 = System.currentTimeMillis() - start1;

        // Second call - should use cache
        long start2 = System.currentTimeMillis();
        ClickStatsResponse stats2 = analyticsService.getClickStats(urlMapping.getShortCode());
        long time2 = System.currentTimeMillis() - start2;

        // Cache should be faster
        assertTrue(time2 < time1);
        assertEquals(stats1.getTotalClicks(), stats2.getTotalClicks());
    }
    
    // Helper methods
    private UrlMapping createTestUrlMapping() {
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setShortCode("test" + System.currentTimeMillis());
        urlMapping.setOriginalUrl("https://example.com/test");
        return urlMappingRepository.save(urlMapping);
    }
    
    private void createTestClickEvents(UrlMapping urlMapping, int count) {
        // This is a placeholder - in a real test you would create ClickEvent objects
        // For now, this method exists to prevent compilation errors
        // You can implement this when you have ClickEvent creation logic
    }
}
