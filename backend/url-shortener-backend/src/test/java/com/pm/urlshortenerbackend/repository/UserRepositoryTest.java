package com.pm.urlshortenerbackend.repository;

import com.pm.urlshortenerbackend.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_WhenUserExists_ShouldReturnUser() {
        // Given
        User user = new User("test@example.com", "hashedPassword", "John", "Doe");
        entityManager.persistAndFlush(user);

        // When
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getFirstName()).isEqualTo("John");
    }

    @Test
    void findByEmail_WhenUserDoesNotExist_ShouldReturnEmpty() {
        // When
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void existsByEmail_WhenUserExists_ShouldReturnTrue() {
        // Given
        User user = new User("test@example.com", "hashedPassword", "John", "Doe");
        entityManager.persistAndFlush(user);

        // When
        boolean exists = userRepository.existsByEmail("test@example.com");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_WhenUserDoesNotExist_ShouldReturnFalse() {
        // When
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void findByEmailIgnoreCase_ShouldFindUserRegardlessOfCase() {
        // Given
        User user = new User("Test@Example.COM", "hashedPassword", "John", "Doe");
        entityManager.persistAndFlush(user);

        // When
        Optional<User> found = userRepository.findByEmailIgnoreCase("test@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("Test@Example.COM");
    }

    @Test
    void existsByEmailIgnoreCase_ShouldReturnTrueRegardlessOfCase() {
        // Given
        User user = new User("Test@Example.COM", "hashedPassword", "John", "Doe");
        entityManager.persistAndFlush(user);

        // When
        boolean exists = userRepository.existsByEmailIgnoreCase("test@example.com");

        // Then
        assertThat(exists).isTrue();
    }
}
