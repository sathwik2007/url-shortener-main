package com.pm.urlshortenerbackend.service;

import com.pm.urlshortenerbackend.model.User;
import com.pm.urlshortenerbackend.repository.UserRepository;
import com.pm.urlshortenerbackend.service.impl.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserDetailsServiceIntegrationTest {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void loadUserByUsername_WithRealDatabase_ShouldWork() {
        // Given
        User user = new User("integration@example.com", "hashedPassword", "Integration", "Test");
        userRepository.save(user);

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("integration@example.com");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("integration@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("hashedPassword");
        assertThat(userDetails.getAuthorities()).hasSize(1);
    }

    @Test
    void loadUserByUsername_WithNonExistentUser_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
