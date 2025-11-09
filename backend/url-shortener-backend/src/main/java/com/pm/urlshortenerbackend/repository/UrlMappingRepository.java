package com.pm.urlshortenerbackend.repository;

import com.pm.urlshortenerbackend.model.UrlMapping;
import com.pm.urlshortenerbackend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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

    Page<UrlMapping> findByOwner(User owner, Pageable pageable);

    Page<UrlMapping> findByOwnerAndIsActive(User owner, Boolean isActive, Pageable pageable);

    Optional<UrlMapping> findByShortCodeAndOwner(String shortCode, User owner);

    @Query("SELECT u FROM UrlMapping u WHERE u.expiresAt IS NOT NULL AND u.expiresAt < :now")
    List<UrlMapping> findExpiredUrls(@Param("now") LocalDateTime now);

    @Query("SELECT u FROM UrlMapping u WHERE u.expiresAt IS NOT NULL AND u.expiresAt BETWEEN :now AND :threshold")
    List<UrlMapping> findUrlsExpiringSoon(@Param("now") LocalDateTime now, @Param("threshold") LocalDateTime threshold);

    long countByOwner(User owner);

    long countByOwnerAndIsActive(User owner, Boolean isActive);

    @Modifying
    @Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + 1 WHERE u.shortCode = :shortCode")
    int incrementClickCount(@Param("shortCode") String shortCode);

    @Modifying
    @Query("UPDATE UrlMapping u SET u.isActive = false WHERE u.expiresAt IS NOT NULL AND u.expiresAt < :now AND u.isActive = true")
    int deactivateExpiredUrls(@Param("now") LocalDateTime now);

    //Method to find anonymous URLs (URLs with no owner)
    Page<UrlMapping> findByOwnerIsNull(Pageable pageable);

    @Query("SELECT u FROM UrlMapping u WHERE u.owner = :owner AND u.clickCount > :minClicks ORDER BY u.clickCount DESC")
    List<UrlMapping> findPopularUrlsByOwner(@Param("owner") User owner, @Param("minClicks") Long minClicks);

    Optional<UrlMapping> findByOriginalUrlAndOwner(String originalUrl, User owner);
}
