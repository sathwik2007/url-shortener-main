package com.pm.urlshortenerbackend.exception;

/**
 * Author: Sathwik Pillalamarri
 * Date: 12/9/25
 * Project: url-shortener-backend
 */
public class AnalyticsException extends RuntimeException{

    public AnalyticsException(String message) {
        super(message);
    }

    public AnalyticsException(String message, Throwable cause) {
        super(message, cause);
    }
}
