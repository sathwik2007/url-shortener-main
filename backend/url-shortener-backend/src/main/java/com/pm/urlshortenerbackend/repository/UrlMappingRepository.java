package com.pm.urlshortenerbackend.repository;

import com.pm.urlshortenerbackend.model.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Author: sathwikpillalamarri
 * Date: 9/13/25
 * Project: url-shortener-backend
 */
@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {
    Optional<UrlMapping> findByShortCode(String shortCode);
    Optional<UrlMapping> findByOriginalUrl(String originalUrl);
}
