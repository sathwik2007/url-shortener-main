package com.pm.urlshortenerbackend.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Author: Sathwik Pillalamarri
 * Date: 10/7/25
 * Project: url-shortener-backend
 */
@Component
public class RedisHealthIndicator implements HealthIndicator {
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.health.redis.timeout:5000}")
    private long timeout;

    public RedisHealthIndicator(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try{
            String result = redisTemplate.execute((RedisCallback<String>) RedisConnectionCommands::ping);

            if("PONG".equals(result)) {
                return Health.up()
                        .withDetail("redis", "available")
                        .withDetail("response", result)
                        .build();
            } else {
                return Health.down()
                        .withDetail("redis", "Unexpected Response")
                        .withDetail("response", result)
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("redis", "Connection Failed")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
