package com.pm.urlshortenerbackend.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RedisHealthIndicator
 * 
 * Author: Sathwik Pillalamarri
 * Date: 10/7/25
 * Project: url-shortener-backend
 */
@ExtendWith(MockitoExtension.class)
class RedisHealthIndicatorTest {
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @InjectMocks
    private RedisHealthIndicator redisHealthIndicator;
    
    @Test
    void testHealthUp_WhenRedisRespondsWithPong() {
        // Arrange
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn("PONG");
        
        // Act
        Health health = redisHealthIndicator.health();
        
        // Assert
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("redis", "available");
        assertThat(health.getDetails()).containsEntry("response", "PONG");
    }
    
    @Test
    void testHealthDown_WhenRedisRespondsWithUnexpectedValue() {
        // Arrange
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn("UNEXPECTED");
        
        // Act
        Health health = redisHealthIndicator.health();
        
        // Assert
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("redis", "Unexpected Response");
        assertThat(health.getDetails()).containsEntry("response", "UNEXPECTED");
    }
    
    @Test
    void testHealthDown_WhenRedisThrowsException() {
        // Arrange
        when(redisTemplate.execute(any(RedisCallback.class)))
            .thenThrow(new RuntimeException("Connection failed"));
        
        // Act
        Health health = redisHealthIndicator.health();
        
        // Assert
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("redis", "Connection Failed");
        assertThat(health.getDetails()).containsKey("error");
        assertThat(health.getDetails().get("error")).isEqualTo("Connection failed");
    }
    
    @Test
    void testHealthDown_WhenRedisReturnsNull() {
        // Arrange - RedisTemplate throws exception when null is returned
        when(redisTemplate.execute(any(RedisCallback.class)))
            .thenThrow(new IllegalArgumentException("'value' must not be null"));
        
        // Act
        Health health = redisHealthIndicator.health();
        
        // Assert
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("redis", "Connection Failed");
        assertThat(health.getDetails()).containsKey("error");
    }
}