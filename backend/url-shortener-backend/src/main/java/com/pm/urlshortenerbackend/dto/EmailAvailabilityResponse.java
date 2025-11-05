package com.pm.urlshortenerbackend.dto;

/**
 * Author: Sathwik Pillalamarri
 * Date: 11/5/25
 * Project: url-shortener-backend
 */
public class EmailAvailabilityResponse {
    private String email;
    private boolean available;

    public EmailAvailabilityResponse() {}

    public EmailAvailabilityResponse(String email, boolean available) {
        this.email = email;
        this.available = available;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {
        return "EmailAvailabilityResponse{" +
                "email='" + email + '\'' +
                ", available=" + available +
                '}';
    }
}
