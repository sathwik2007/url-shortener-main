package com.pm.urlshortenerbackend.service.impl;

import com.pm.urlshortenerbackend.dto.ClickEventData;
import com.pm.urlshortenerbackend.health.UrlShortenerMetrics;
import com.pm.urlshortenerbackend.model.ClickEvent;
import com.pm.urlshortenerbackend.model.UrlMapping;
import com.pm.urlshortenerbackend.repository.ClickEventRepository;
import com.pm.urlshortenerbackend.repository.UrlMappingRepository;
import com.pm.urlshortenerbackend.service.ClickTrackingService;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

/**
 * Author: Sathwik Pillalamarri
 * Date: 11/29/25
 * Project: url-shortener-backend
 */
@Service
public class ClickTrackingServiceImpl implements ClickTrackingService {
    private static final Logger log = LoggerFactory.getLogger(ClickTrackingServiceImpl.class);
    private final ClickEventRepository clickEventRepository;
    private final UrlMappingRepository urlMappingRepository;
    private final UrlShortenerMetrics metrics;

    public ClickTrackingServiceImpl(ClickEventRepository clickEventRepository,
                                    UrlMappingRepository urlMappingRepository,
                                    UrlShortenerMetrics metrics) {
        this.clickEventRepository = clickEventRepository;
        this.urlMappingRepository = urlMappingRepository;
        this.metrics = metrics;
    }

    @Override
    @Async("clickTrackingExecutor")
    @Transactional
    public void logClick(String shortCode, ClickEventData clickEventData) {
        Timer.Sample sample = metrics.startClickTrackingTimer();
        try {
            log.debug("Logging click for shortCode: {}", shortCode);

            Optional<UrlMapping> urlMappingOpt = urlMappingRepository.findByShortCode(shortCode);
            if(urlMappingOpt.isEmpty()) {
                log.warn("URL mapping not found for shortCode: {}", shortCode);
                return;
            }
            UrlMapping urlMapping = urlMappingOpt.get();

            // Create a click event
            ClickEvent clickEvent = new ClickEvent(urlMapping);

            // Use pre-extracted data instead of accessing the HttpServlet request
            clickEvent.setIpAddressHash(hashIpAddress(clickEventData.getIpAddress()));
            clickEvent.setUserAgent(clickEventData.getUserAgent());
            clickEvent.setReferrer(clickEventData.getReferrer());

            //Parse metadata from user agent
            clickEvent.setDeviceType(extractDeviceType(clickEventData.getUserAgent()));
            clickEvent.setBrowser(extractBrowser(clickEventData.getUserAgent()));
            clickEvent.setOperatingSystem(extractOperatingSystem(clickEventData.getUserAgent()));

            clickEventRepository.save(clickEvent);

            urlMapping.incrementClickCount();
            urlMappingRepository.save(urlMapping);

            metrics.incrementClickTrackingSuccess();
            log.debug("Successfully logged click for shortCode: {}", shortCode);
        } catch (Exception e) {
            metrics.incrementClickTrackingFailure();
            log.error("Error logging click for shortCode: {}", shortCode, e);
        } finally {
            metrics.recordClickTrackingTime(sample);
        }
    }

    // Hash IP address using SHA-256 for privacy
    private String hashIpAddress(String ipAddress) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ipAddress.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for(byte b: hash) {
                String hex = Integer.toHexString(0xff & b);
                if(hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Error hashing IP address", e);
            return "hash_error";
        }
    }

    // Extract device type from the user agent
    private String extractDeviceType(String userAgent) {
        if(userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        String ua = userAgent.toLowerCase();

        if(ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return "Mobile";
        }

        if(ua.contains("tablet") || ua.contains("ipad")) {
            return "Tablet";
        }

        if(ua.contains("bot") || ua.contains("crawler") || ua.contains("spider")) {
            return "Bot";
        }

        return "Desktop";
    }

    // Extract browser from user agent
    private String extractBrowser(String userAgent) {
        if(userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        String ua = userAgent.toLowerCase();

        if(ua.contains("edg/") || ua.contains("edge")) {
            return "Edge";
        }
        if(ua.contains("opr/") || ua.contains("opera")) {
            return "Opera";
        }
        if(ua.contains("chrome") && !ua.contains("edg/")) {
            return "Chrome";
        }
        if(ua.contains("safari") && !ua.contains("chrome")) {
            return "Safari";
        }
        if(ua.contains("firefox")) {
            return "Firefox";
        }
        return "Other";
    }

    // Extract OS from user agent
    private String extractOperatingSystem(String userAgent) {
        if(userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        String ua = userAgent.toLowerCase();

        if(ua.contains("android")) {
            return "Android";
        }

        if(ua.contains("iphone") || ua.contains("ipad")) {
            return "iOS";
        }

        if(ua.contains("windows nt 10")) {
            return "Windows 10/11";
        }
        if(ua.contains("windows nt 6.3")) {
            return "Windows 8.1";
        }
        if(ua.contains("windows nt 6.2")) {
            return "Windows 8";
        }
        if(ua.contains("windows nt 6.1")) {
            return "Windows 7";
        }
        if(ua.contains("windows")) {
            return "Windows";
        }
        if(ua.contains("mac os x")) {
            return "macOS";
        }
        if(ua.contains("linux")) {
            return "Linux";
        }

        return "Other";
    }
}
