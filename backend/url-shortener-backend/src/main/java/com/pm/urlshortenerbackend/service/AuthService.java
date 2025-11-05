package com.pm.urlshortenerbackend.service;

import com.pm.urlshortenerbackend.dto.AuthResponse;
import com.pm.urlshortenerbackend.dto.LoginRequest;
import com.pm.urlshortenerbackend.dto.RegisterRequest;

/**
 * Author: Sathwik Pillalamarri
 * Date: 11/4/25
 * Project: url-shortener-backend
 */
public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    boolean isEmailAvailable(String email);
}
