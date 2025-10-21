package com.pm.urlshortenerbackend.controller;

import com.pm.urlshortenerbackend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class TestController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @PostMapping("/test-login")
    public ResponseEntity<?> testLogin(@RequestBody Map<String, String> request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.get("email"),
                            request.get("password")
                    )
            );

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.get("email"));
            String token = jwtUtil.generateToken(userDetails);

            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("email", userDetails.getUsername());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid credentials");
        }
    }

    @GetMapping("/test-protected")
    public ResponseEntity<String> testProtected() {
        return ResponseEntity.ok("This is a protected endpoint - JWT is working!");
    }
}
