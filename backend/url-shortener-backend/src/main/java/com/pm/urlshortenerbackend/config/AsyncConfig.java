package com.pm.urlshortenerbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Author: Sathwik Pillalamarri
 * Date: 11/29/25
 * Project: url-shortener-backend
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "clickTrackingExecutor")
    public Executor clickTrackingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size - minimum threads to keep alive
        executor.setCorePoolSize(5);

        // Max pool size - maximum threads to create
        executor.setMaxPoolSize(10);

        // Queue capacity - tasks to queue before creating new threads
        executor.setQueueCapacity(100);

        // Thread name prefix(used for debugging)
        executor.setThreadNamePrefix("click-tracking-");

        // Rejection policy - what to do when queue is full
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }
}
