package com.pm.urlshortenerbackend.service.impl;

import com.pm.urlshortenerbackend.repository.UrlMappingRepository;
import com.pm.urlshortenerbackend.service.IdGenerationService;
import com.pm.urlshortenerbackend.util.Base62Util;
import org.springframework.stereotype.Service;

/**
 * Author: sathwikpillalamarri
 * Date: 9/14/25
 * Project: url-shortener-backend
 */
@Service
public class IdGenerationServiceImpl implements IdGenerationService {
    private final UrlMappingRepository urlMappingRepository;

    public IdGenerationServiceImpl(UrlMappingRepository urlMappingRepository) {
        this.urlMappingRepository = urlMappingRepository;
    }


    @Override
    public String generateUniqueId(long value) {
        String shortCode = Base62Util.encode(value);
        long currentValue = value;

        while(urlMappingRepository.findByShortCode(shortCode).isPresent()) {
            currentValue++;
            shortCode = Base62Util.encode(currentValue);
        }
        return shortCode;
    }

    @Override
    public String generateUniqueShortCode() {
        long uniqueId = System.currentTimeMillis() + (long)(Math.random() * 10000);
        String shortCode = Base62Util.encode(uniqueId);

        // Check for collisions and retry if needed
        long currentValue = uniqueId;
        while(urlMappingRepository.findByShortCode(shortCode).isPresent()) {
            currentValue++;
            shortCode = Base62Util.encode(currentValue);
        }
        return shortCode;
    }
}
