package com.pm.urlshortenerbackend.service;

import com.pm.urlshortenerbackend.model.ClickEvent;
import com.pm.urlshortenerbackend.model.UrlMapping;
import com.pm.urlshortenerbackend.repository.ClickEventRepository;
import com.pm.urlshortenerbackend.repository.UrlMappingRepository;
import com.pm.urlshortenerbackend.service.impl.ClickTrackingServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Author: Sathwik Pillalamarri
 * Date: 11/29/25
 * Project: url-shortener-backend
 */
@SpringBootTest
public class ClickTrackingServiceTest {

    @Autowired
    private ClickTrackingServiceImpl clickTrackingService;

    @Autowired
    private ClickEventRepository clickEventRepository;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testLogClick() throws Exception {
        // Create test URL mapping
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setShortCode("test123");
        urlMapping.setOriginalUrl("https://example.com");
        urlMapping = urlMappingRepository.save(urlMapping);

        // Mock request
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/91.0");
        when(request.getHeader("Referer")).thenReturn("https://google.com");

        // Log click
        clickTrackingService.logClick("test123", request);

        // Wait for async operation
        Thread.sleep(1000);

        // Verify click event was created
        List<ClickEvent> events = clickEventRepository.findByUrlMapping(urlMapping);
        assertEquals(1, events.size());

        ClickEvent event = events.get(0);
        assertNotNull(event.getIpAddressHash());
        assertEquals("Desktop", event.getDeviceType());
        assertEquals("Chrome", event.getBrowser());
    }
}
