package com.pm.urlshortenerbackend.exception;

/**
 * Author: sathwikpillalamarri
 * Date: 9/28/25
 * Project: url-shortener-backend
 */
public class InvalidUrlException extends RuntimeException{
    public InvalidUrlException(String message) {
        super(message);
    }
}
