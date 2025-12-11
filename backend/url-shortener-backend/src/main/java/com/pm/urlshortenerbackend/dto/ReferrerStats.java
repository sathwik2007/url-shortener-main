package com.pm.urlshortenerbackend.dto;

import java.util.List;

/**
 * Author: Sathwik Pillalamarri
 * Date: 12/8/25
 * Project: url-shortener-backend
 */
public class ReferrerStats {
    private List<CategoryStats> referrer;

    public ReferrerStats() {
    }

    public ReferrerStats(List<CategoryStats> referrer) {
        this.referrer = referrer;
    }

    public List<CategoryStats> getReferrer() {
        return referrer;
    }

    public void setReferrer(List<CategoryStats> referrer) {
        this.referrer = referrer;
    }
}
