package com.pm.urlshortenerbackend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Author: Sathwik Pillalamarri
 * Date: 11/12/25
 * Project: url-shortener-backend
 */
@Entity
@Table(name = "click_events", indexes = {
        @Index(name = "idx_click_events_urlmapping_id", columnList = "url_mapping_id"),
        @Index(name = "idx_click_events_clicked_at", columnList = "clicked_at"),
        @Index(name = "idx_click_events_url_mapping_clicked_at", columnList = "url_mapping_id,clicked_at")
})
public class ClickEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_mapping_id", nullable = false)
    private UrlMapping urlMapping;

    @CreationTimestamp
    @Column(name = "clicked_at", nullable = false, updatable = false)
    private LocalDateTime clickedAt;

    @Column(name = "ip_address_hash", length = 64)
    private String ipAddressHash;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "referrer", length = 500)
    private String referrer;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "browser", length = 100)
    private String browser;

    @Column(name = "operating_system", length = 100)
    private String operatingSystem;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "city", length = 100)
    private String city;

    public ClickEvent() {
    }

    public ClickEvent(UrlMapping urlMapping) {
        this.urlMapping = urlMapping;
    }

    public ClickEvent(UrlMapping urlMapping, String ipAddressHash, String userAgent, String referrer) {
        this.urlMapping = urlMapping;
        this.ipAddressHash = ipAddressHash;
        this.userAgent = userAgent;
        this.referrer = referrer;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UrlMapping getUrlMapping() {
        return urlMapping;
    }

    public void setUrlMapping(UrlMapping urlMapping) {
        this.urlMapping = urlMapping;
    }

    public LocalDateTime getClickedAt() {
        return clickedAt;
    }

    public void setClickedAt(LocalDateTime clickedAt) {
        this.clickedAt = clickedAt;
    }

    public String getIpAddressHash() {
        return ipAddressHash;
    }

    public void setIpAddressHash(String ipAddressHash) {
        this.ipAddressHash = ipAddressHash;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClickEvent)) return false;
        ClickEvent that = (ClickEvent) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "ClickEvent{" +
                "id=" + id +
                ", urlMapping=" + (urlMapping != null ? urlMapping : null) +
                ", clickedAt=" + clickedAt +
                ", deviceType='" + deviceType + '\'' +
                ", browser='" + browser + '\'' +
                '}';
    }
}
