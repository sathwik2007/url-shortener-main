package com.pm.urlshortenerbackend.dto;

import java.util.List;

/**
 * Author: Sathwik Pillalamarri
 * Date: 12/8/25
 * Project: url-shortener-backend
 */
public class ClickStatsResponse {
    private String shortCode;
    private String OriginalUrl;
    private long totalClicks;
    private List<DailyClickStats> dailyStats;
    private DeviceStats deviceStats;
    private BrowserStats browserStats;
    private CountryStats countryStats;
    private ReferrerStats referrerStats;

    public ClickStatsResponse() {
    }

    public ClickStatsResponse(String shortCode, String originalUrl, long totalClicks) {
        this.shortCode = shortCode;
        OriginalUrl = originalUrl;
        this.totalClicks = totalClicks;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getOriginalUrl() {
        return OriginalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        OriginalUrl = originalUrl;
    }

    public long getTotalClicks() {
        return totalClicks;
    }

    public void setTotalClicks(long totalClicks) {
        this.totalClicks = totalClicks;
    }

    public List<DailyClickStats> getDailyStats() {
        return dailyStats;
    }

    public void setDailyStats(List<DailyClickStats> dailyStats) {
        this.dailyStats = dailyStats;
    }

    public DeviceStats getDeviceStats() {
        return deviceStats;
    }

    public void setDeviceStats(DeviceStats deviceStats) {
        this.deviceStats = deviceStats;
    }

    public BrowserStats getBrowserStats() {
        return browserStats;
    }

    public void setBrowserStats(BrowserStats browserStats) {
        this.browserStats = browserStats;
    }

    public CountryStats getCountryStats() {
        return countryStats;
    }

    public void setCountryStats(CountryStats countryStats) {
        this.countryStats = countryStats;
    }

    public ReferrerStats getReferrerStats() {
        return referrerStats;
    }

    public void setReferrerStats(ReferrerStats referrerStats) {
        this.referrerStats = referrerStats;
    }
}
