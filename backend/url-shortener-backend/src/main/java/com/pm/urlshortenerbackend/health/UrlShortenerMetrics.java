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
    private final Counter urlDeactivationCounter;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter errorCounter;
    private final Counter clickTrackingSuccessCounter;
    private final Counter clickTrackingFailureCounter;
    private final Counter analyticsRequestCounter;
    private final Counter analyticsCacheHitCounter;
    private final Counter analyticsCacheMissCounter;
    private final Timer clickTrackingTimer;
    private final Timer urlCreationTimer;
    private final Timer urlRetrievalTimer;
    private final Timer redirectTimer;
    private final Timer analyticsCalculationTimer;
    
    public UrlShortenerMetrics(MeterRegistry meterRegistry) {
        this.urlCreationCounter = Counter.builder("url_shortener_urls_created_total")
            .description("Total number of URLs created")
            .register(meterRegistry);
            
        this.urlRetrievalCounter = Counter.builder("url_shortener_urls_retrieved_total")
            .description("Total number of URLs retrieved")
            .register(meterRegistry);

        this.urlDeactivationCounter = Counter.builder("url_deactivation_total")
                .description("Total number of URLs deactivated due to expiration")
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

        this.clickTrackingSuccessCounter = Counter.builder("click_tracking_success_total")
                .description("Total number of successful click tracking operations")
                .register(meterRegistry);

        this.clickTrackingFailureCounter = Counter.builder("click_tracking_failure_total")
                .description("Total number of failed click tracking operations")
                .register(meterRegistry);

        this.analyticsRequestCounter = Counter.builder("analytics_request_total")
                .description("Total number of analytics requests")
                .register(meterRegistry);

        this.analyticsCacheHitCounter = Counter.builder("analytics_cache_hit_total")
                .description("Total number of analytics cache hits")
                .register(meterRegistry);

        this.analyticsCacheMissCounter = Counter.builder("analytics_cache_misses_total")
                .description("Total number of analytics cache misses")
                .register(meterRegistry);
            
        this.urlCreationTimer = Timer.builder("url_shortener_url_creation_duration")
            .description("Time taken to create URLs")
            .register(meterRegistry);
            
        this.urlRetrievalTimer = Timer.builder("url_shortener_url_retrieval_duration")
            .description("Time taken to retrieve URLs")
            .register(meterRegistry);

        this.clickTrackingTimer = Timer.builder("click_tracking_duration")
                .description("Time taken to log click events")
                .register(meterRegistry);

        this.redirectTimer = Timer.builder("url_redirect_duration")
                .description("Time taken to process redirect requests")
                .register(meterRegistry);

        this.analyticsCalculationTimer = Timer.builder("analytics_calculation_duration")
                .description("Time to calculate analytics")
                .register(meterRegistry);
    }
    
    public void incrementUrlCreation() {
        urlCreationCounter.increment();
    }
    
    public void incrementUrlRetrieval() {
        urlRetrievalCounter.increment();
    }

    public void incrementUrlDeactivation(int count) { urlDeactivationCounter.increment(count); }
    
    public void incrementCacheHit() {
        cacheHitCounter.increment();
    }
    
    public void incrementCacheMiss() {
        cacheMissCounter.increment();
    }
    
    public void incrementError() {
        errorCounter.increment();
    }

    public void incrementClickTrackingSuccess() {clickTrackingSuccessCounter.increment(); }

    public void incrementClickTrackingFailure() { clickTrackingFailureCounter.increment(); }

    public void incrementAnalyticsRequest() { analyticsRequestCounter.increment(); }

    public void incrementAnalyticsCacheHit() { analyticsCacheHitCounter.increment(); }

    public void incrementAnalyticsCacheMiss() { analyticsCacheMissCounter.increment(); }
    
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

    public Timer.Sample startClickTrackingTimer() { return Timer.start(); }

    public void recordClickTrackingTime(Timer.Sample sample) { sample.stop(clickTrackingTimer); }

    public Timer.Sample startRedirectTimer() { return Timer.start(); }

    public void recordRedirectTime(Timer.Sample sample) { sample.stop(redirectTimer); }

    public Timer.Sample startAnalyticsCalculationTimer() { return Timer.start(); }

    public void recordAnalyticsCalculationTime(Timer.Sample sample) { sample.stop(analyticsCalculationTimer); }
}