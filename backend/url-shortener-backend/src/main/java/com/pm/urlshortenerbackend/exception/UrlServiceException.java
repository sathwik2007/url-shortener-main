package com.pm.urlshortenerbackend.exception;

/**
 * Author: sathwikpillalamarri
 * Date: 9/28/25
 * Project: url-shortener-backend
 */
public class UrlServiceException extends RuntimeException{
    public UrlServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
