package com.pm.urlshortenerbackend.health;

import com.pm.urlshortenerbackend.repository.UrlMappingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DatabaseHealthIndicator
 * 
 * Author: Sathwik Pillalamarri
 * Date: 10/7/25
 * Project: url-shortener-backend
 */
@ExtendWith(MockitoExtension.class)
class DatabaseHealthIndicatorTest {
    
    @Mock
    private UrlMappingRepository urlMappingRepository;
    
    @InjectMocks
    private DatabaseHealthIndicator databaseHealthIndicator;
    
    @Test
    void testHealthUp_WhenDatabaseIsAccessible() {
        // Arrange
        long expectedCount = 42L;
        when(urlMappingRepository.count()).thenReturn(expectedCount);
        
        // Act
        Health health = databaseHealthIndicator.health();
        
        // Assert
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("database", "Available");
        assertThat(health.getDetails()).containsEntry("totalUrls", expectedCount);
        assertThat(health.getDetails()).containsEntry("status", "Connected");
    }
    
    @Test
    void testHealthUp_WhenDatabaseIsEmptyButAccessible() {
        // Arrange
        when(urlMappingRepository.count()).thenReturn(0L);
        
        // Act
        Health health = databaseHealthIndicator.health();
        
        // Assert
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("database", "Available");
        assertThat(health.getDetails()).containsEntry("totalUrls", 0L);
        assertThat(health.getDetails()).containsEntry("status", "Connected");
    }
    
    @Test
    void testHealthDown_WhenDatabaseThrowsException() {
        // Arrange
        when(urlMappingRepository.count())
            .thenThrow(new RuntimeException("Database connection failed"));
        
        // Act
        Health health = databaseHealthIndicator.health();
        
        // Assert
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("database", "Connection Failed");
        assertThat(health.getDetails()).containsKey("error");
        assertThat(health.getDetails().get("error")).isEqualTo("Database connection failed");
    }
    
    @Test
    void testHealthDown_WhenDatabaseThrowsDataAccessException() {
        // Arrange
        when(urlMappingRepository.count())
            .thenThrow(new org.springframework.dao.DataAccessException("Connection timeout") {});
        
        // Act
        Health health = databaseHealthIndicator.health();
        
        // Assert
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("database", "Connection Failed");
        assertThat(health.getDetails()).containsKey("error");
        assertThat(health.getDetails().get("error")).isEqualTo("Connection timeout");
    }
}