package com.pm.urlshortenerbackend.service.impl;

import com.pm.urlshortenerbackend.dto.AuthResponse;
import com.pm.urlshortenerbackend.dto.LoginRequest;
import com.pm.urlshortenerbackend.dto.RegisterRequest;
import com.pm.urlshortenerbackend.exception.EmailAlreadyExistsException;
import com.pm.urlshortenerbackend.exception.InvalidCredentialsException;
import com.pm.urlshortenerbackend.model.User;
import com.pm.urlshortenerbackend.repository.UserRepository;
import com.pm.urlshortenerbackend.security.JwtUtil;
import com.pm.urlshortenerbackend.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Author: Sathwik Pillalamarri
 * Date: 11/4/25
 * Project: url-shortener-backend
 */
@Service
@Transactional
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        log.info("Attempting to register user with email: {}", request.getEmail());

        if(userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            log.warn("Registration failed - email already exists: {}", request.getEmail());
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {} and email: {}", savedUser.getId(), savedUser.getEmail());

        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        return buildAuthResponse(savedUser, token);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Attempting to authenticate user with email: {}", request.getEmail());

        //Finding the  user by email
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found: {}", request.getEmail());
                    return new InvalidCredentialsException("Invalid email or password");
                });

        //User successfully fetched - validate the password
        if(!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed - invalid password for user: {}", request.getEmail());
            throw new InvalidCredentialsException("Invalid email or password");
        }

        log.info("User authenticated successfully: {}", user.getEmail());

        //User is successfully authenticated - generate a JWT token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);
        return buildAuthResponse(user, token);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmailIgnoreCase(email);
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getCreatedAt()
        );
    }
}
