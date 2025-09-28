package com.pm.urlshortenerbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Author: sathwikpillalamarri
 * Date: 9/28/25
 * Project: url-shortener-backend
 */
public class CreateUrlRequest {
    @NotBlank(message = "Original URL is required")
    @Size(max = 2048, message = "URL exceeds maximum allowed length")
    @Pattern(
            regexp = "^(http|https)://.*$",
            message = "URL must start with http:// or https://"
    )
    private String originalUrl;

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    @Override
    public String toString() {
        return "CreateUrlRequest{" +
                "originalUrl='" + originalUrl + '\'' +
                '}';
    }
}
