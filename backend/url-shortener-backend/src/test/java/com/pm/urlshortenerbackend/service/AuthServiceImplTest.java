package com.pm.urlshortenerbackend.service;

import com.pm.urlshortenerbackend.dto.AuthResponse;
import com.pm.urlshortenerbackend.dto.LoginRequest;
import com.pm.urlshortenerbackend.dto.RegisterRequest;
import com.pm.urlshortenerbackend.exception.EmailAlreadyExistsException;
import com.pm.urlshortenerbackend.exception.InvalidCredentialsException;
import com.pm.urlshortenerbackend.model.User;
import com.pm.urlshortenerbackend.repository.UserRepository;
import com.pm.urlshortenerbackend.security.JwtUtil;
import com.pm.urlshortenerbackend.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private UserDetails userDetails;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtUtil, userDetailsService);
    }

    @Test
    void register_WithValidRequest_ShouldReturnAuthResponse() {
        // Given
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "John", "Doe");
        User savedUser = new User("test@example.com", "hashedPassword", "John", "Doe");
        savedUser.setId(1L);
        savedUser.setCreatedAt(LocalDateTime.now());

        when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn("jwt-token");

        // When
        AuthResponse response = authService.register(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getType()).isEqualTo("Bearer");
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");

        verify(userRepository).existsByEmailIgnoreCase("test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(jwtUtil).generateToken(userDetails);
    }

    @Test
    void register_WithExistingEmail_ShouldThrowException() {
        // Given
        RegisterRequest request = new RegisterRequest("existing@example.com", "password123", "John", "Doe");
        when(userRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Email already exists: existing@example.com");

        verify(userRepository).existsByEmailIgnoreCase("existing@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_WithValidCredentials_ShouldReturnAuthResponse() {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        User user = new User("test@example.com", "hashedPassword", "John", "Doe");
        user.setId(1L);
        user.setCreatedAt(LocalDateTime.now());

        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn("jwt-token");

        // When
        AuthResponse response = authService.login(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("test@example.com");

        verify(userRepository).findByEmailIgnoreCase("test@example.com");
        verify(passwordEncoder).matches("password123", "hashedPassword");
        verify(jwtUtil).generateToken(userDetails);
    }

    @Test
    void login_WithInvalidEmail_ShouldThrowException() {
        // Given
        LoginRequest request = new LoginRequest("nonexistent@example.com", "password123");
        when(userRepository.findByEmailIgnoreCase("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(userRepository).findByEmailIgnoreCase("nonexistent@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_WithInvalidPassword_ShouldThrowException() {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");
        User user = new User("test@example.com", "hashedPassword", "John", "Doe");

        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "hashedPassword")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(userRepository).findByEmailIgnoreCase("test@example.com");
        verify(passwordEncoder).matches("wrongpassword", "hashedPassword");
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void isEmailAvailable_WithAvailableEmail_ShouldReturnTrue() {
        // Given
        when(userRepository.existsByEmailIgnoreCase("available@example.com")).thenReturn(false);

        // When
        boolean result = authService.isEmailAvailable("available@example.com");

        // Then
        assertThat(result).isTrue();
        verify(userRepository).existsByEmailIgnoreCase("available@example.com");
    }

    @Test
    void isEmailAvailable_WithExistingEmail_ShouldReturnFalse() {
        // Given
        when(userRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

        // When
        boolean result = authService.isEmailAvailable("existing@example.com");

        // Then
        assertThat(result).isFalse();
        verify(userRepository).existsByEmailIgnoreCase("existing@example.com");
    }
}
