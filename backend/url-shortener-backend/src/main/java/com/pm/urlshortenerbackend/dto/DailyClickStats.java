package com.pm.urlshortenerbackend.dto;

import java.time.LocalDate;

/**
 * Author: Sathwik Pillalamarri
 * Date: 11/29/25
 * Project: url-shortener-backend
 */
public class DailyClickStats {
    private LocalDate date;
    private Long clickCount;

    public DailyClickStats(LocalDate date, Long clickCount) {
        this.date = date;
        this.clickCount = clickCount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getClickCount() {
        return clickCount;
    }

    public void setClickCount(Long clickCount) {
        this.clickCount = clickCount;
    }
}
