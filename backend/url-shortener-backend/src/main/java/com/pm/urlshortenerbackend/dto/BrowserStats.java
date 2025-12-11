package com.pm.urlshortenerbackend.dto;

import java.util.List;

/**
 * Author: Sathwik Pillalamarri
 * Date: 12/8/25
 * Project: url-shortener-backend
 */
public class BrowserStats {
    private List<CategoryStats> browsers;

    public BrowserStats() {
    }

    public BrowserStats(List<CategoryStats> browsers) {
        this.browsers = browsers;
    }

    public List<CategoryStats> getBrowsers() {
        return browsers;
    }

    public void setBrowsers(List<CategoryStats> browsers) {
        this.browsers = browsers;
    }
}
