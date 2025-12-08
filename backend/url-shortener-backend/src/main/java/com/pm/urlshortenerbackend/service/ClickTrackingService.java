package com.pm.urlshortenerbackend.service;

import com.pm.urlshortenerbackend.dto.ClickEventData;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Author: Sathwik Pillalamarri
 * Date: 11/29/25
 * Project: url-shortener-backend
 */
public interface ClickTrackingService {
    void logClick(String shortCode, ClickEventData clickEventData);
}
