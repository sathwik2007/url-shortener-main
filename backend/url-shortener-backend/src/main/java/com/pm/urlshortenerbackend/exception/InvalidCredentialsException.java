package com.pm.urlshortenerbackend.exception;

/**
 * Author: Sathwik Pillalamarri
 * Date: 11/4/25
 * Project: url-shortener-backend
 */
public class InvalidCredentialsException extends RuntimeException{
    public InvalidCredentialsException(String message) {
        super(message);
    }

    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
