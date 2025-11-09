package com.pm.urlshortenerbackend.exception;

/**
 * Author: Sathwik Pillalamarri
 * Date: 11/6/25
 * Project: url-shortener-backend
 */
public class UnauthorizedAccessException extends RuntimeException{
    public UnauthorizedAccessException(String message) {
        super(message);
    }

    public UnauthorizedAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
