package com.pm.urlshortenerbackend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Author: sathwikpillalamarri
 * Date: 9/13/25
 * Project: url-shortener-backend
 */
@Entity
@SequenceGenerator(name = "url_seq", sequenceName = "url_sequence", allocationSize = 1)
@Table(name = "url_mappings", indexes = {
        @Index(name = "idx_url_mappings_owner_id", columnList = "owner_id"),
        @Index(name = "idx_url_mappings_expires_at", columnList = "expires_at"),
        @Index(name = "idx_url_mappings_short_code", columnList = "shortCode"),
        @Index(name = "idx_url_mappings_is_active", columnList = "is_active")
})
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = true)
    private User owner;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "click_count", nullable = true)
    private Long clickCount = 0L;

    @Column(name = "is_active", nullable = true)
    private Boolean isActive = true;

    public UrlMapping() {
    }

    public UrlMapping(String shortCode, String originalUrl) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
    }

    public UrlMapping(String shortCode, String originalUrl, User owner) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.owner = owner;
    }

    public UrlMapping(String shortCode, String originalUrl, User owner, LocalDateTime expiresAt) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.owner = owner;
        this.expiresAt = expiresAt;
    }

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

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getClickCount() {
        return clickCount;
    }

    public void setClickCount(Long clickCount) {
        this.clickCount = clickCount;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    //Utility Methods
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isAccessible() {
        return isActive && !isExpired();
    }

    public void incrementClickCount() {
        this.clickCount = (this.clickCount == null ? 0L : this.clickCount) + 1L;
    }

    public boolean isOwnedBy(User user) {
        return owner != null && user != null && owner.getId().equals(user.getId());
    }

    public boolean isAnonymous() {
        return owner == null;
    }

    @Override
    public String toString() {
        return "UrlMapping{" +
                "id=" + id +
                ", shortCode='" + shortCode + '\'' +
                ", originalUrl='" + originalUrl + '\'' +
                ", createdAt=" + createdAt +
                ", owner=" + owner +
                ", expiresAt=" + expiresAt +
                ", clickCount=" + clickCount +
                ", isActive=" + isActive +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UrlMapping that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
