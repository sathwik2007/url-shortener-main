package com.pm.urlshortenerbackend.health;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UrlShortenerMetrics
 * 
 * Author: Sathwik Pillalamarri
 * Date: 10/7/25
 * Project: url-shortener-backend
 */
class UrlShortenerMetricsTest {
    
    private MeterRegistry meterRegistry;
    private UrlShortenerMetrics metrics;
    
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metrics = new UrlShortenerMetrics(meterRegistry);
    }
    
    @Test
    void testIncrementUrlCreation() {
        // Act
        metrics.incrementUrlCreation();
        metrics.incrementUrlCreation();
        
        // Assert
        Counter counter = meterRegistry.find("url_shortener_urls_created_total").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }
    
    @Test
    void testIncrementUrlRetrieval() {
        // Act
        metrics.incrementUrlRetrieval();
        metrics.incrementUrlRetrieval();
        metrics.incrementUrlRetrieval();
        
        // Assert
        Counter counter = meterRegistry.find("url_shortener_urls_retrieved_total").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(3.0);
    }
    
    @Test
    void testIncrementCacheHit() {
        // Act
        metrics.incrementCacheHit();
        
        // Assert
        Counter counter = meterRegistry.find("url_shortener_cache_hits_total").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
    
    @Test
    void testIncrementCacheMiss() {
        // Act
        metrics.incrementCacheMiss();
        metrics.incrementCacheMiss();
        
        // Assert
        Counter counter = meterRegistry.find("url_shortener_cache_misses_total").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }
    
    @Test
    void testIncrementError() {
        // Act
        metrics.incrementError();
        
        // Assert
        Counter counter = meterRegistry.find("url_shortener_errors_total").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
    
    @Test
    void testUrlCreationTimer() {
        // Act
        Timer.Sample sample = metrics.startUrlCreationTimer();
        
        // Simulate some work
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        metrics.recordUrlCreationTime(sample);
        
        // Assert
        Timer timer = meterRegistry.find("url_shortener_url_creation_duration").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(0);
    }
    
    @Test
    void testUrlRetrievalTimer() {
        // Act
        Timer.Sample sample = metrics.startUrlRetrievalTimer();
        
        // Simulate some work
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        metrics.recordUrlRetrievalTime(sample);
        
        // Assert
        Timer timer = meterRegistry.find("url_shortener_url_retrieval_duration").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(0);
    }
    
    @Test
    void testMultipleOperations() {
        // Act - Simulate various operations
        metrics.incrementUrlCreation();
        metrics.incrementUrlRetrieval();
        metrics.incrementCacheHit();
        metrics.incrementCacheMiss();
        metrics.incrementError();
        
        Timer.Sample creationSample = metrics.startUrlCreationTimer();
        metrics.recordUrlCreationTime(creationSample);
        
        Timer.Sample retrievalSample = metrics.startUrlRetrievalTimer();
        metrics.recordUrlRetrievalTime(retrievalSample);
        
        // Assert all counters
        assertThat(meterRegistry.find("url_shortener_urls_created_total").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("url_shortener_urls_retrieved_total").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("url_shortener_cache_hits_total").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("url_shortener_cache_misses_total").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("url_shortener_errors_total").counter().count()).isEqualTo(1.0);
        
        // Assert timers
        assertThat(meterRegistry.find("url_shortener_url_creation_duration").timer().count()).isEqualTo(1);
        assertThat(meterRegistry.find("url_shortener_url_retrieval_duration").timer().count()).isEqualTo(1);
    }
}