package com.pm.urlshortenerbackend.health;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Custom metrics for monitoring URL shortener application performance
 * 
 * Author: Sathwik Pillalamarri
 * Date: 10/7/25
 * Project: url-shortener-backend
 */
@Component
public class UrlShortenerMetrics {
    
    private final Counter urlCreationCounter;
    private final Counter urlRetrievalCounter;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter errorCounter;
    private final Timer urlCreationTimer;
    private final Timer urlRetrievalTimer;
    
    public UrlShortenerMetrics(MeterRegistry meterRegistry) {
        this.urlCreationCounter = Counter.builder("url_shortener_urls_created_total")
            .description("Total number of URLs created")
            .register(meterRegistry);
            
        this.urlRetrievalCounter = Counter.builder("url_shortener_urls_retrieved_total")
            .description("Total number of URLs retrieved")
            .register(meterRegistry);
            
        this.cacheHitCounter = Counter.builder("url_shortener_cache_hits_total")
            .description("Total number of cache hits")
            .register(meterRegistry);
            
        this.cacheMissCounter = Counter.builder("url_shortener_cache_misses_total")
            .description("Total number of cache misses")
            .register(meterRegistry);
            
        this.errorCounter = Counter.builder("url_shortener_errors_total")
            .description("Total number of errors")
            .register(meterRegistry);
            
        this.urlCreationTimer = Timer.builder("url_shortener_url_creation_duration")
            .description("Time taken to create URLs")
            .register(meterRegistry);
            
        this.urlRetrievalTimer = Timer.builder("url_shortener_url_retrieval_duration")
            .description("Time taken to retrieve URLs")
            .register(meterRegistry);
    }
    
    public void incrementUrlCreation() {
        urlCreationCounter.increment();
    }
    
    public void incrementUrlRetrieval() {
        urlRetrievalCounter.increment();
    }
    
    public void incrementCacheHit() {
        cacheHitCounter.increment();
    }
    
    public void incrementCacheMiss() {
        cacheMissCounter.increment();
    }
    
    public void incrementError() {
        errorCounter.increment();
    }
    
    public Timer.Sample startUrlCreationTimer() {
        return Timer.start();
    }
    
    public void recordUrlCreationTime(Timer.Sample sample) {
        sample.stop(urlCreationTimer);
    }
    
    public Timer.Sample startUrlRetrievalTimer() {
        return Timer.start();
    }
    
    public void recordUrlRetrievalTime(Timer.Sample sample) {
        sample.stop(urlRetrievalTimer);
    }
}