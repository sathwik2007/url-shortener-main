package com.pm.urlshortenerbackend.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class UrlMappingTest {

    @Test
    void isExpired_WhenNoExpirationSet_ShouldReturnFalse() {
        UrlMapping urlMapping = new UrlMapping("abc123", "https://example.com");
        assertThat(urlMapping.isExpired()).isFalse();
    }

    @Test
    void isExpired_WhenExpirationInFuture_ShouldReturnFalse() {
        UrlMapping urlMapping = new UrlMapping("abc123", "https://example.com");
        urlMapping.setExpiresAt(LocalDateTime.now().plusHours(1));
        assertThat(urlMapping.isExpired()).isFalse();
    }

    @Test
    void isExpired_WhenExpirationInPast_ShouldReturnTrue() {
        UrlMapping urlMapping = new UrlMapping("abc123", "https://example.com");
        urlMapping.setExpiresAt(LocalDateTime.now().minusHours(1));
        assertThat(urlMapping.isExpired()).isTrue();
    }

    @Test
    void isAccessible_WhenActiveAndNotExpired_ShouldReturnTrue() {
        UrlMapping urlMapping = new UrlMapping("abc123", "https://example.com");
        urlMapping.setIsActive(true);
        urlMapping.setExpiresAt(LocalDateTime.now().plusHours(1));
        assertThat(urlMapping.isAccessible()).isTrue();
    }

    @Test
    void isAccessible_WhenInactive_ShouldReturnFalse() {
        UrlMapping urlMapping = new UrlMapping("abc123", "https://example.com");
        urlMapping.setIsActive(false);
        assertThat(urlMapping.isAccessible()).isFalse();
    }

    @Test
    void incrementClickCount_ShouldIncreaseCount() {
        UrlMapping urlMapping = new UrlMapping("abc123", "https://example.com");
        assertThat(urlMapping.getClickCount()).isEqualTo(0L);

        urlMapping.incrementClickCount();
        assertThat(urlMapping.getClickCount()).isEqualTo(1L);

        urlMapping.incrementClickCount();
        assertThat(urlMapping.getClickCount()).isEqualTo(2L);
    }

    @Test
    void isOwnedBy_WhenSameUser_ShouldReturnTrue() {
        User user = new User("test@example.com", "password", "John", "Doe");
        user.setId(1L);

        UrlMapping urlMapping = new UrlMapping("abc123", "https://example.com", user);
        assertThat(urlMapping.isOwnedBy(user)).isTrue();
    }

    @Test
    void isAnonymous_WhenNoOwner_ShouldReturnTrue() {
        UrlMapping urlMapping = new UrlMapping("abc123", "https://example.com");
        assertThat(urlMapping.isAnonymous()).isTrue();
    }

    @Test
    void isAnonymous_WhenHasOwner_ShouldReturnFalse() {
        User user = new User("test@example.com", "password", "John", "Doe");
        UrlMapping urlMapping = new UrlMapping("abc123", "https://example.com", user);
        assertThat(urlMapping.isAnonymous()).isFalse();
    }
}
