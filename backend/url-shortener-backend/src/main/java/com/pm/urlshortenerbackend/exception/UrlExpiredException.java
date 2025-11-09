package com.pm.urlshortenerbackend.exception;

/**
 * Author: Sathwik Pillalamarri
 * Date: 11/6/25
 * Project: url-shortener-backend
 */
public class UrlExpiredException extends RuntimeException {
    private final String shortCode;

    public UrlExpiredException(String shortCode) {
        super("URL with short code '" + shortCode + "' has expired");
        this.shortCode = shortCode;
    }

    public UrlExpiredException(String shortCode, String message) {
        super(message);
        this.shortCode = shortCode;
    }

    public String getShortCode() {
        return shortCode;
    }
}
