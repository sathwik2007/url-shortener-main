package com.pm.urlshortenerbackend.service;

import com.pm.urlshortenerbackend.dto.ClickStatsResponse;
import com.pm.urlshortenerbackend.dto.DailyClickStats;
import com.pm.urlshortenerbackend.model.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Author: Sathwik Pillalamarri
 * Date: 12/8/25
 * Project: url-shortener-backend
 */
public interface AnalyticsService {
    // Get click statistics for a URL
    ClickStatsResponse getClickStats(String shortCode);

    // Get click statistics for a URL with user ownership validation
    ClickStatsResponse getClickStats(String shortCode, User user);

    // Get daily statistics for a URL for last N days
    List<DailyClickStats> getDailyStats(String shortCode, int days);

    // Get daily statistics for a specific date range
    List<DailyClickStats> getDailyStats(String shortCode, LocalDateTime startDate, LocalDateTime endDate);

    // Get total number of clicks for a URL
    long getTotalClicks(String shortCode);

    // Get number of clicks for a specific date range
    long getClickCount(String shortCode, LocalDateTime startDate, LocalDateTime endDate);

    // Refresh cached statistics for a URL
    void refreshCachedStats(String shortCode);

    // Clear all cached statistics
    void clearAllCachedStats();
}
