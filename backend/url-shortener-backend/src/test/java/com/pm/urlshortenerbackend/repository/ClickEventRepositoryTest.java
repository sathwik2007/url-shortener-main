package com.pm.urlshortenerbackend.repository;

import com.pm.urlshortenerbackend.model.ClickEvent;
import com.pm.urlshortenerbackend.model.UrlMapping;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SpringBootTest
@Transactional
public class ClickEventRepositoryTest {

    @Autowired
    private ClickEventRepository clickEventRepository;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    private UrlMapping testUrlMapping;

    @BeforeEach
    void setUp() {
        testUrlMapping = new UrlMapping();
        testUrlMapping.setShortCode("test123");
        testUrlMapping.setOriginalUrl("https://example.com");
        testUrlMapping = urlMappingRepository.save(testUrlMapping);
    }

    @Test
    void testSaveClickEvent() {
        ClickEvent clickEvent = new ClickEvent(testUrlMapping);
        clickEvent.setIpAddressHash("hashed_ip");
        clickEvent.setUserAgent("Mozilla/5.0");
        clickEvent.setDeviceType("Desktop");

        ClickEvent saved = clickEventRepository.save(clickEvent);

        assertNotNull(saved.getId());
        assertNotNull(saved.getClickedAt());
        assertEquals("hashed_ip", saved.getIpAddressHash());
    }

    @Test
    void testCountByUrlMapping() {
        // Create multiple click events
        for (int i = 0; i < 5; i++) {
            ClickEvent event = new ClickEvent(testUrlMapping);
            clickEventRepository.save(event);
        }

        long count = clickEventRepository.countByUrlMapping(testUrlMapping);
        assertEquals(5, count);
    }

    @Test
    void testFindByUrlMappingAndDateRange() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime tomorrow = now.plusDays(1);

        ClickEvent event = new ClickEvent(testUrlMapping);
        clickEventRepository.save(event);

        List<ClickEvent> events = clickEventRepository.findByUrlMappingAndDateRange(
                testUrlMapping, yesterday, tomorrow
        );

        assertEquals(1, events.size());
    }
}
