package com.pm.urlshortenerbackend.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

/**
 * Component that verifies all dependencies during application startup
 * 
 * Author: Sathwik Pillalamarri
 * Date: 10/7/25
 * Project: url-shortener-backend
 */
@Component
public class StartupHealthVerifier implements ApplicationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(StartupHealthVerifier.class);
    
    private final RedisHealthIndicator redisHealthIndicator;
    private final DatabaseHealthIndicator databaseHealthIndicator;
    
    public StartupHealthVerifier(RedisHealthIndicator redisHealthIndicator,
                               DatabaseHealthIndicator databaseHealthIndicator) {
        this.redisHealthIndicator = redisHealthIndicator;
        this.databaseHealthIndicator = databaseHealthIndicator;
    }
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("Starting application health verification...");
        
        // Verify Redis connectivity
        Health redisHealth = redisHealthIndicator.health();
        if (redisHealth.getStatus() == Status.UP) {
            logger.info("✓ Redis connection verified successfully");
        } else {
            logger.error("✗ Redis connection failed: {}", redisHealth.getDetails());
        }
        
        // Verify Database connectivity
        Health dbHealth = databaseHealthIndicator.health();
        if (dbHealth.getStatus() == Status.UP) {
            logger.info("✓ Database connection verified successfully");
        } else {
            logger.error("✗ Database connection failed: {}", dbHealth.getDetails());
        }
        
        logger.info("Application health verification completed");
        logger.info("Application is ready to serve requests");
    }
}