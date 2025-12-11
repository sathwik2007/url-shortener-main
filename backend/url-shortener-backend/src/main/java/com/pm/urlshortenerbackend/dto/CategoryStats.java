package com.pm.urlshortenerbackend.dto;

/**
 * Author: Sathwik Pillalamarri
 * Date: 11/29/25
 * Project: url-shortener-backend
 */
public class CategoryStats {
    private String category;
    private long count;
    private double percentage;

    public CategoryStats() {
    }

    public CategoryStats(String category, long count) {
        this.category = category;
        this.count = count;
    }

    public CategoryStats(String category, long count, double percentage) {
        this.category = category;
        this.count = count;
        this.percentage = percentage;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
}
