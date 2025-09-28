package com.pm.urlshortenerbackend.exception;

/**
 * Author: sathwikpillalamarri
 * Date: 9/28/25
 * Project: url-shortener-backend
 */
public class UrlNotFoundException extends RuntimeException{
    public UrlNotFoundException(String shortCode) {
        super("URL not found for code: " + shortCode);
    }
}
