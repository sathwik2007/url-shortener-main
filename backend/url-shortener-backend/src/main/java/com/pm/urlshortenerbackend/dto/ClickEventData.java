package com.pm.urlshortenerbackend.dto;

/**
 * Author: Sathwik Pillalamarri
 * Date: 12/3/25
 * Project: url-shortener-backend
 */
public class ClickEventData {
    private final String ipAddress;
    private final String userAgent;
    private final String referrer;

    public ClickEventData(String ipAddress, String userAgent, String referrer) {
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.referrer = referrer;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getReferrer() {
        return referrer;
    }

    public String getUserAgent() {
        return userAgent;
    }
}
