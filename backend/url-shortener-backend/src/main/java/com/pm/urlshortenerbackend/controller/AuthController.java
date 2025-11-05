package com.pm.urlshortenerbackend.controller;

import com.pm.urlshortenerbackend.dto.AuthResponse;
import com.pm.urlshortenerbackend.dto.EmailAvailabilityResponse;
import com.pm.urlshortenerbackend.dto.LoginRequest;
import com.pm.urlshortenerbackend.dto.RegisterRequest;
import com.pm.urlshortenerbackend.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Author: Sathwik Pillalamarri
 * Date: 11/5/25
 * Project: url-shortener-backend
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(originPatterns = "*", maxAge = 3600)
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    //End point for registering a new user
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for email: {}", request.getEmail());

        long startTime = System.currentTimeMillis();

        try {
            AuthResponse response = authService.register(request);
            long duration = System.currentTimeMillis() - startTime;

            log.info("User registered successfully: {} in {} ms", response.getEmail(), duration);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            long duration  = System.currentTimeMillis() - startTime;
            log.warn("Registration failed for email: {} in {} ms - {}", request.getEmail(), duration, e.getMessage());
            throw e;
        }
    }

    //End point for authenticating a login request
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for email: {}", request.getEmail());

        long startTime = System.currentTimeMillis();

        try {
            AuthResponse response = authService.login(request);
            long duration = System.currentTimeMillis() - startTime;

            log.info("User logged in successfully: {} in {} ms", response.getEmail(), duration);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("Login failed for email: {} in {} ms - {}", request.getEmail(), duration, e.getMessage());
            throw e;
        }
    }

    //End point for checking if the email is available for registration
    @GetMapping("/check-email")
    public ResponseEntity<EmailAvailabilityResponse> checkEmailAvailability(@RequestParam String email) {
        log.debug("Email availability check for: {}", email);

        boolean available = authService.isEmailAvailable(email);
        EmailAvailabilityResponse response = new EmailAvailabilityResponse(email, available);

        return ResponseEntity.ok(response);
    }
}
