package com.pm.urlshortenerbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.urlshortenerbackend.dto.AuthResponse;
import com.pm.urlshortenerbackend.dto.LoginRequest;
import com.pm.urlshortenerbackend.dto.RegisterRequest;
import com.pm.urlshortenerbackend.exception.EmailAlreadyExistsException;
import com.pm.urlshortenerbackend.exception.InvalidCredentialsException;
import com.pm.urlshortenerbackend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_WithValidRequest_ShouldReturn201Created() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "John", "Doe");
        AuthResponse response = new AuthResponse("jwt-token", 1L, "test@example.com", "John", "Doe", LocalDateTime.now());

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    void register_WithExistingEmail_ShouldReturn400BadRequest() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("existing@example.com", "password123", "John", "Doe");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("existing@example.com"));

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already exists: existing@example.com"));
    }

    @Test
    void register_WithInvalidRequest_ShouldReturn400BadRequest() throws Exception {
        // Given - Invalid request (missing required fields)
        RegisterRequest request = new RegisterRequest("", "", "", "");

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.password").exists())
                .andExpect(jsonPath("$.firstName").exists())
                .andExpect(jsonPath("$.lastName").exists());
    }

    @Test
    void register_WithInvalidEmail_ShouldReturn400BadRequest() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("invalid-email", "password123", "John", "Doe");

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Email should be valid"));
    }

    @Test
    void register_WithShortPassword_ShouldReturn400BadRequest() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("test@example.com", "123", "John", "Doe");

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").value("Password must be between 8 and 100 characters"));
    }

    @Test
    void login_WithValidCredentials_ShouldReturn200OK() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        AuthResponse response = new AuthResponse("jwt-token", 1L, "test@example.com", "John", "Doe", LocalDateTime.now());

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void login_WithInvalidCredentials_ShouldReturn401Unauthorized() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    void login_WithInvalidRequest_ShouldReturn400BadRequest() throws Exception {
        // Given - Invalid request (missing email)
        LoginRequest request = new LoginRequest("", "password123");

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void checkEmailAvailability_WithAvailableEmail_ShouldReturn200OK() throws Exception {
        // Given
        when(authService.isEmailAvailable("available@example.com")).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/auth/check-email")
                        .param("email", "available@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("available@example.com"))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void checkEmailAvailability_WithExistingEmail_ShouldReturn200OK() throws Exception {
        // Given
        when(authService.isEmailAvailable("existing@example.com")).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/auth/check-email")
                        .param("email", "existing@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("existing@example.com"))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void register_WithMissingContentType_ShouldReturn415UnsupportedMediaType() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "John", "Doe");

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void register_WithInvalidJson_ShouldReturn400BadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andExpect(status().isBadRequest());
    }
}
