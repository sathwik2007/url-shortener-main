package com.pm.urlshortenerbackend.dto;

/**
 * Author: Sathwik Pillalamarri
 * Date: 12/11/25
 * Project: url-shortener-backend
 */
public class UserUrlSummary {
    private long totalUrls;
    private long activeUrls;
    private long inactiveUrls;

    public UserUrlSummary(long totalUrls, long activeUrls) {
        this.totalUrls = totalUrls;
        this.activeUrls = activeUrls;
        this.inactiveUrls = totalUrls - activeUrls;
    }

    public long getTotalUrls() {
        return totalUrls;
    }

    public void setTotalUrls(long totalUrls) {
        this.totalUrls = totalUrls;
    }

    public long getActiveUrls() {
        return activeUrls;
    }

    public void setActiveUrls(long activeUrls) {
        this.activeUrls = activeUrls;
    }

    public long getInactiveUrls() {
        return inactiveUrls;
    }

    public void setInactiveUrls(long inactiveUrls) {
        this.inactiveUrls = inactiveUrls;
    }
}
