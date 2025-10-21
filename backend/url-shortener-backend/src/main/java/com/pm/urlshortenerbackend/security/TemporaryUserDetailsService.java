package com.pm.urlshortenerbackend.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;

public class TemporaryUserDetailsService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;

    public TemporaryUserDetailsService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        if ("test@example.com".equals(username)) {
            return new User("test@example.com",
                    passwordEncoder.encode("password"),
                    new ArrayList<>());
        }
        throw new UsernameNotFoundException("User not found with email: " + username);
    }
}
