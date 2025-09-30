package com.pm.urlshortenerbackend.service;

import com.pm.urlshortenerbackend.model.UrlMapping;
import com.pm.urlshortenerbackend.repository.UrlMappingRepository;
import com.pm.urlshortenerbackend.service.impl.IdGenerationServiceImpl;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Author: sathwikpillalamarri
 * Date: 9/14/25
 * Project: url-shortener-backend
 */
@SpringBootTest
public class IdGenerationServiceImplTest {
    @Autowired
    private IdGenerationServiceImpl idGenerationServiceImpl;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @Test
    void testUniqueIdGeneration() {
        String shortCode1 = idGenerationServiceImpl.generateUniqueId(125L);
        String shortCode2 = idGenerationServiceImpl.generateUniqueId(126L);

        assertThat(shortCode1).isNotEqualTo(shortCode2);
    }

    @Test
    void testGenerateUniqueIdWithZero() {
        String shortCode = idGenerationServiceImpl.generateUniqueId(0L);
        assertThat(shortCode).isNotEmpty();
    }

    @Test
    @Transactional
    @Rollback
    void testCollisionDetection() {
        UrlMapping existingUrl = new UrlMapping();
        existingUrl.setOriginalUrl("https://www.existing.com");
        existingUrl.setShortCode("abc123");
        urlMappingRepository.save(existingUrl);

        String newShortCode = idGenerationServiceImpl.generateUniqueId(125L);
        assertThat(newShortCode).isNotEqualTo("abc123");
    }
}
