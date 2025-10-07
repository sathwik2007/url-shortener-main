package com.pm.urlshortenerbackend.health;

import com.pm.urlshortenerbackend.repository.UrlMappingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.info.Info;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UrlShortenerInfoContributor
 * 
 * Author: Sathwik Pillalamarri
 * Date: 10/7/25
 * Project: url-shortener-backend
 */
@ExtendWith(MockitoExtension.class)
class UrlShortenerInfoContributorTest {
    
    @Mock
    private UrlMappingRepository urlMappingRepository;
    
    @InjectMocks
    private UrlShortenerInfoContributor infoContributor;
    
    @Test
    void testContribute_WhenRepositoryIsAccessible() {
        // Arrange
        long expectedCount = 100L;
        when(urlMappingRepository.count()).thenReturn(expectedCount);
        
        Info.Builder builder = new Info.Builder();
        
        // Act
        infoContributor.contribute(builder);
        Info info = builder.build();
        
        // Assert
        Map<String, Object> details = info.getDetails();
        
        // Check application details
        assertThat(details).containsKey("application");
        @SuppressWarnings("unchecked")
        Map<String, Object> appDetails = (Map<String, Object>) details.get("application");
        assertThat(appDetails).containsEntry("name", "URL Shortener");
        assertThat(appDetails).containsEntry("version", "1.0.0");
        assertThat(appDetails).containsEntry("description", "A simple URL shortening service");
        
        // Check statistics
        assertThat(details).containsKey("statistics");
        @SuppressWarnings("unchecked")
        Map<String, Object> statistics = (Map<String, Object>) details.get("statistics");
        assertThat(statistics).containsEntry("totalUrls", expectedCount);
        assertThat(statistics).containsKey("uptime");
        assertThat(statistics.get("uptime")).isInstanceOf(Long.class);
    }
    
    @Test
    void testContribute_WhenRepositoryThrowsException() {
        // Arrange
        when(urlMappingRepository.count())
            .thenThrow(new RuntimeException("Database error"));
        
        Info.Builder builder = new Info.Builder();
        
        // Act
        infoContributor.contribute(builder);
        Info info = builder.build();
        
        // Assert
        Map<String, Object> details = info.getDetails();
        
        // Check application details are still present
        assertThat(details).containsKey("application");
        
        // Check error in statistics
        assertThat(details).containsKey("statistics");
        @SuppressWarnings("unchecked")
        Map<String, Object> statistics = (Map<String, Object>) details.get("statistics");
        assertThat(statistics).containsKey("error");
        assertThat(statistics.get("error")).isEqualTo("Unable to fetch statistics: Database error");
    }
    
    @Test
    void testContribute_WhenRepositoryReturnsZero() {
        // Arrange
        when(urlMappingRepository.count()).thenReturn(0L);
        
        Info.Builder builder = new Info.Builder();
        
        // Act
        infoContributor.contribute(builder);
        Info info = builder.build();
        
        // Assert
        Map<String, Object> details = info.getDetails();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> statistics = (Map<String, Object>) details.get("statistics");
        assertThat(statistics).containsEntry("totalUrls", 0L);
        assertThat(statistics).containsKey("uptime");
    }
}