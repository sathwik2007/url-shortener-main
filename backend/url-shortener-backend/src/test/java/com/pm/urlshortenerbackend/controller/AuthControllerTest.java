package com.pm.urlshortenerbackend.controller;

import com.pm.urlshortenerbackend.dto.AuthResponse;
import com.pm.urlshortenerbackend.dto.EmailAvailabilityResponse;
import com.pm.urlshortenerbackend.dto.LoginRequest;
import com.pm.urlshortenerbackend.dto.RegisterRequest;
import com.pm.urlshortenerbackend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService);
    }

    @Test
    void register_ShouldReturnCreatedStatus() {
        // Given
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "John", "Doe");
        AuthResponse authResponse = new AuthResponse("jwt-token", 1L, "test@example.com", "John", "Doe", LocalDateTime.now());

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        // When
        ResponseEntity<AuthResponse> response = authController.register(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isEqualTo("jwt-token");
        assertThat(response.getBody().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void login_ShouldReturnOkStatus() {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        AuthResponse authResponse = new AuthResponse("jwt-token", 1L, "test@example.com", "John", "Doe", LocalDateTime.now());

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // When
        ResponseEntity<AuthResponse> response = authController.login(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isEqualTo("jwt-token");
        assertThat(response.getBody().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void checkEmailAvailability_WithAvailableEmail_ShouldReturnTrue() {
        // Given
        when(authService.isEmailAvailable("available@example.com")).thenReturn(true);

        // When
        ResponseEntity<EmailAvailabilityResponse> response = authController.checkEmailAvailability("available@example.com");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEmail()).isEqualTo("available@example.com");
        assertThat(response.getBody().isAvailable()).isTrue();
    }

    @Test
    void checkEmailAvailability_WithExistingEmail_ShouldReturnFalse() {
        // Given
        when(authService.isEmailAvailable("existing@example.com")).thenReturn(false);

        // When
        ResponseEntity<EmailAvailabilityResponse> response = authController.checkEmailAvailability("existing@example.com");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEmail()).isEqualTo("existing@example.com");
        assertThat(response.getBody().isAvailable()).isFalse();
    }
}
