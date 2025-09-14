package com.pm.urlshortenerbackend.repository;

import com.pm.urlshortenerbackend.model.UrlMapping;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Author: sathwikpillalamarri
 * Date: 9/13/25
 * Project: url-shortener-backend
 */
@DataJpaTest
public class UrlMappingRepositoryTest {
    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @Test
    void findByShortCodeTest() {
        UrlMapping url = new UrlMapping();
        url.setOriginalUrl("https://www.google.com");
        url.setShortCode("abcde");

        testEntityManager.persist(url);
        testEntityManager.flush();

        Optional<UrlMapping> found = urlMappingRepository.findByShortCode(url.getShortCode());
        assertThat(found).isPresent();
        assertThat(found.get().getOriginalUrl()).isEqualTo("https://www.google.com");
    }

    @Test
    void findByShortCodeTest_NotFound() {
        Optional<UrlMapping> found = urlMappingRepository.findByShortCode("nonexistent");
        assertThat(found).isEmpty();
    }

    @Test
    void findByOriginalUrlTest() {
        UrlMapping url = new UrlMapping();
        url.setOriginalUrl("https://www.google.com");
        url.setShortCode("abcde");

        testEntityManager.persist(url);
        testEntityManager.flush();

        Optional<UrlMapping> found = urlMappingRepository.findByOriginalUrl(url.getOriginalUrl());
        assertThat(found).isPresent();
        assertThat(found.get().getShortCode()).isEqualTo("abcde");
    }

    @Test
    void findByOriginalUrlTest_NotFound() {
        Optional<UrlMapping> found = urlMappingRepository.findByOriginalUrl("https://www.nonexistent.com");
        assertThat(found).isEmpty();
    }
}
