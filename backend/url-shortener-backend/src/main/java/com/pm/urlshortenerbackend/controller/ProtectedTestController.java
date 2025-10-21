package com.pm.urlshortenerbackend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Author: Sathwik Pillalamarri
 * Date: 10/16/25
 * Project: url-shortener-backend
 */
@RestController
@RequestMapping("/api")
public class ProtectedTestController {

    @GetMapping("/test-protected")
    public ResponseEntity<String> testProtected() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return ResponseEntity.ok("This is a protected endpoint - JWT is working! Authenticated user: " + username);
    }
}