package com.pm.urlshortenerbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Author: Sathwik Pillalamarri
 * Date: 11/4/25
 * Project: url-shortener-backend
 */
public class LoginRequest {
    @NotBlank(message = "email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "password must be required")
    private String password;

    public LoginRequest() {}

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "LoginRequest{" +
                "email='" + email + '\'' +
                '}';
    }
}
