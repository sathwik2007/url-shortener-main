package com.pm.urlshortenerbackend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

/**
 * Author: sathwikpillalamarri
 * Date: 9/13/25
 * Project: url-shortener-backend
 */
@Entity
@SequenceGenerator(name = "url_seq", sequenceName = "url_sequence", allocationSize = 1)
@Table(name = "url_mappings")
public class UrlMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "url_seq")
    private long id;

    @Column(unique = true, nullable = false)
    private String shortCode;

    @URL(message = "Invalid URL Format")
    @Column(nullable = false, length = 2048)
    private String originalUrl;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "UrlMapping{" +
                "id=" + id +
                ", shortCode='" + shortCode + '\'' +
                ", originalUrl='" + originalUrl + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
