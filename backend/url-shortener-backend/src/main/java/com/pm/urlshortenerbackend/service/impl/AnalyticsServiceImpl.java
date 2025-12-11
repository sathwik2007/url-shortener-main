package com.pm.urlshortenerbackend.service.impl;

import com.pm.urlshortenerbackend.dto.*;
import com.pm.urlshortenerbackend.exception.UrlNotFoundException;
import com.pm.urlshortenerbackend.model.UrlMapping;
import com.pm.urlshortenerbackend.model.User;
import com.pm.urlshortenerbackend.repository.ClickEventRepository;
import com.pm.urlshortenerbackend.repository.UrlMappingRepository;
import com.pm.urlshortenerbackend.service.AnalyticsService;
import com.pm.urlshortenerbackend.service.CacheService;
import com.pm.urlshortenerbackend.service.UrlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Author: Sathwik Pillalamarri
 * Date: 12/8/25
 * Project: url-shortener-backend
 */
@Service
public class AnalyticsServiceImpl implements AnalyticsService {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsServiceImpl.class);
    private final ClickEventRepository clickEventRepository;
    private final UrlMappingRepository urlMappingRepository;
    private final UrlService urlService;
    private final CacheService cacheService;

    @Value("${app.analytics.cache-ttl:1800}")
    private long analyticsCacheTtl;

    @Value("${app.analytics.default-days:7}")
    private int defaultDays;

    public AnalyticsServiceImpl(ClickEventRepository clickEventRepository, UrlMappingRepository urlMappingRepository, UrlService urlService, CacheService cacheService) {
        this.clickEventRepository = clickEventRepository;
        this.urlMappingRepository = urlMappingRepository;
        this.urlService = urlService;
        this.cacheService = cacheService;
    }

    @Override
    public ClickStatsResponse getClickStats(String shortCode) {
        log.debug("Getting click statistics for shortCode: {}", shortCode);

        // Trying the cache first for analytics
        String cacheKey = "stats:" + shortCode;
        Optional<ClickStatsResponse> cached = cacheService.getUrlMapping(cacheKey, ClickStatsResponse.class);
        if(cached.isPresent()) {
            log.debug("Cache hit for analytics: {}", shortCode);
            return cached.get();
        }

        UrlMapping urlMapping = getUrlMapping(shortCode);

        ClickStatsResponse stats = calculateClickStats(urlMapping);

        cacheService.putUrlMapping(cacheKey, stats, analyticsCacheTtl);
        return stats;
    }

    @Override
    public ClickStatsResponse getClickStats(String shortCode, User user) {
        log.debug("Getting click statistics for shortCode: {} with user validation", shortCode);

        urlService.validateUrlOwnership(shortCode, user);
        return getClickStats(shortCode);
    }

    @Override
    public List<DailyClickStats> getDailyStats(String shortCode, int days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);
        return getDailyStats(shortCode, startDate, endDate);
    }

    @Override
    public List<DailyClickStats> getDailyStats(String shortCode, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Getting daily statistics for shortCode: {} from {} to {}", shortCode, startDate, endDate);

        UrlMapping urlMapping = getUrlMapping(shortCode);

        List<Object[]> dailyCounts = clickEventRepository.getDailyClickCounts(urlMapping, startDate, endDate);
        return dailyCounts.stream()
                .map(row -> new DailyClickStats(
                        ((Date) row[0]).toLocalDate(),
                        ((Number) row[1]).longValue()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public long getTotalClicks(String shortCode) {
        log.debug("Getting total clicks for shortCode: {}", shortCode);

        UrlMapping urlMapping = getUrlMapping(shortCode);
        return clickEventRepository.countByUrlMapping(urlMapping);
    }

    @Override
    public long getClickCount(String shortCode, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Getting click count for shortCode: {} from {} to {}", shortCode, startDate, endDate);

        UrlMapping urlMapping = getUrlMapping(shortCode);
        return clickEventRepository.countByUrlMappingAndDateRange(urlMapping, startDate, endDate);
    }

    @Override
    public void refreshCachedStats(String shortCode) {
        log.debug("Refreshing cached statistics for shortCode: {}", shortCode);

        String cacheKey = "stats:" + shortCode;
        cacheService.delete(cacheKey);

        getClickStats(shortCode);
    }

    @Override
    public void clearAllCachedStats() {
        log.info("Clearing all cached analytics statistics");

        cacheService.evictPattern("stats:");
    }

    private UrlMapping getUrlMapping(String shortCode) {
        return urlMappingRepository.findByShortCode(shortCode).orElseThrow(() -> new UrlNotFoundException(shortCode));
    }

    private ClickStatsResponse calculateClickStats(UrlMapping urlMapping) {
        String shortCode = urlMapping.getShortCode();

        long totalClicks = clickEventRepository.countByUrlMapping(urlMapping);

        ClickStatsResponse stats = new ClickStatsResponse(shortCode, urlMapping.getOriginalUrl(), totalClicks);

        // Daily stats for last 7 days
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(defaultDays);
        List<DailyClickStats> dailyStats = getDailyStats(shortCode, startDate, endDate);
        stats.setDailyStats(dailyStats);

        // Device stats
        List<Object[]> deviceData = clickEventRepository.getClicksByDeviceType(urlMapping);
        DeviceStats deviceStats = new DeviceStats(convertToCategories(deviceData, totalClicks));
        stats.setDeviceStats(deviceStats);

        // Browser stats
        List<Object[]> browserData = clickEventRepository.getClicksByBrowser(urlMapping);
        BrowserStats browserStats = new BrowserStats(convertToCategories(browserData, totalClicks));
        stats.setBrowserStats(browserStats);

        // Country stats
        List<Object[]> countryData = clickEventRepository.getClicksByCountry(urlMapping);
        CountryStats countryStats = new CountryStats(convertToCategories(countryData, totalClicks));
        stats.setCountryStats(countryStats);

        // Referrer stats
        List<Object[]> referrerData = clickEventRepository.getClicksByReferrer(urlMapping);
        ReferrerStats referrerStats = new ReferrerStats(convertToCategories(referrerData, totalClicks));
        stats.setReferrerStats(referrerStats);

        return stats;
    }

    private List<CategoryStats> convertToCategories(List<Object[]> data, long totalClicks) {
        return data.stream().map(row -> {
            String category = (String) row[0];
            long count = ((Number) row[1]).longValue();
            double percentage = totalClicks > 0 ? (count * 100.0) / totalClicks: 0.0;
            return new CategoryStats(category, count, percentage);
        }).collect(Collectors.toList());
    }
}
