package com.pm.urlshortenerbackend.repository;

import com.pm.urlshortenerbackend.model.ClickEvent;
import com.pm.urlshortenerbackend.model.UrlMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Author: Sathwik Pillalamarri
 * Date: 11/12/25
 * Project: url-shortener-backend
 */
@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    List<ClickEvent> findByUrlMapping(UrlMapping urlMapping);

    // Find all click events for a URL mapping with pagination
    Page<ClickEvent> findByUrlMapping(UrlMapping urlMapping, Pageable pageable);

    // Find all the click events for a URL mapping within a date range
    @Query("SELECT c FROM ClickEvent c WHERE c.urlMapping = :urlMapping " + "AND c.clickedAt BETWEEN :startDate AND :endDate " + "ORDER BY c.clickedAt DESC")
    List<ClickEvent> findByUrlMappingAndDateRange(@Param("urlMapping") UrlMapping urlMapping,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    long countByUrlMapping(UrlMapping urlMapping);

    // Count clicks for a URL mapping within a date range
    @Query("SELECT COUNT(c) as count "+
            "FROM ClickEvent c WHERE c.urlMapping = :urlMapping " +
            "AND c.clickedAt BETWEEN :startDate AND :endDate ")
    long countByUrlMappingAndDateRange(@Param("urlMapping") UrlMapping urlMapping,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    @Query("SELECT DATE(c.clickedAt) as date, COUNT(c) as count " +
            "FROM ClickEvent c WHERE c.urlMapping = :urlMapping " +
            "AND c.clickedAt BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(c.clickedAt) " +
            "ORDER BY DATE(c.clickedAt)")
    List<Object[]> getDailyClickCounts(@Param("urlMapping") UrlMapping urlMapping,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    @Query("SELECT c.deviceType, COUNT(c) FROM ClickEvent c " +
            "WHERE c.urlMapping = :urlMapping " +
            "GROUP BY c.deviceType " +
            "ORDER BY COUNT(c) DESC ")
    List<Object[]> getClicksByDeviceType(@Param("urlMapping") UrlMapping urlMapping);

    @Query("SELECT c.browser, COUNT(c) FROM ClickEvent c " +
            "WHERE c.urlMapping = :urlMapping " +
            "GROUP BY c.browser " +
            "ORDER BY COUNT(c) DESC ")
    List<Object[]> getClicksByBrowser(@Param("urlMapping") UrlMapping urlMapping);

    // Get click counts for a URL Mapping by country
    @Query("SELECT c.country, COUNT(c) FROM ClickEvent c " +
            "WHERE c.urlMapping = :urlMapping " +
            "AND c.country IS NOT NULL " +
            "GROUP BY c.country " +
            "ORDER BY COUNT(c) DESC")
    List<Object[]> getClicksByCountry(@Param("urlMapping") UrlMapping urlMapping);

    @Query("SELECT c FROM ClickEvent c WHERE c.urlMapping = :urlMapping " +
            "ORDER BY c.clickedAt DESC")
    List<ClickEvent> findRecentClicksByUrlMapping(@Param("urlMapping") UrlMapping urlMapping, Pageable pageable);

    // Deleting Older clicks by a cutoff date
    @Query("DELETE FROM ClickEvent c WHERE c.clickedAt < :cutoffDate")
    int deleteClickEventsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT c.referrer, COUNT(c) FROM ClickEvent c " +
            "WHERE c.urlMapping = :urlMapping " +
            "AND c.referrer IS NOT NULL " +
            "GROUP BY c.referrer " +
            "ORDER BY COUNT(c) DESC")
    List<Object[]> getClicksByReferrer(@Param("urlMapping") UrlMapping urlMapping);
}
