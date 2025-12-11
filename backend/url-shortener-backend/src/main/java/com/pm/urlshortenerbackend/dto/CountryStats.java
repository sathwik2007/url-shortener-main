package com.pm.urlshortenerbackend.dto;

import java.util.List;

/**
 * Author: Sathwik Pillalamarri
 * Date: 12/8/25
 * Project: url-shortener-backend
 */
public class CountryStats {
    private List<CategoryStats> countries;

    public CountryStats() {
    }

    public CountryStats(List<CategoryStats> countries) {
        this.countries = countries;
    }

    public List<CategoryStats> getCountries() {
        return countries;
    }

    public void setCountries(List<CategoryStats> countries) {
        this.countries = countries;
    }
}
