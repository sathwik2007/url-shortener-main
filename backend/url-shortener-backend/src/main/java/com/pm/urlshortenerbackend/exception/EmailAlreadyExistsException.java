package com.pm.urlshortenerbackend.exception;

/**
 * Author: Sathwik Pillalamarri
 * Date: 11/4/25
 * Project: url-shortener-backend
 */
public class EmailAlreadyExistsException extends RuntimeException{
    public EmailAlreadyExistsException(String email) {
        super("Email already exists: " + email);
    }

    public EmailAlreadyExistsException(String email, Throwable cause) {
        super("Email already exists: " + email, cause);
    }
}
