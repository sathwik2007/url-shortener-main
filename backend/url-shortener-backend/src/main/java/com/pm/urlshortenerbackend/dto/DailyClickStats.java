package com.pm.urlshortenerbackend.dto;

import java.time.LocalDateTime;

/**
 * Author: Sathwik Pillalamarri
 * Date: 11/29/25
 * Project: url-shortener-backend
 */
public class DailyClickStats {
    private LocalDateTime date;
    private Long clickCount;

    public DailyClickStats(LocalDateTime date, Long clickCount) {
        this.date = date;
        this.clickCount = clickCount;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public Long getClickCount() {
        return clickCount;
    }

    public void setClickCount(Long clickCount) {
        this.clickCount = clickCount;
    }
}
