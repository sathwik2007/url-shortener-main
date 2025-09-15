package com.pm.urlshortenerbackend.service;

import com.pm.urlshortenerbackend.repository.UrlMappingRepository;
import com.pm.urlshortenerbackend.util.Base62Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author: sathwikpillalamarri
 * Date: 9/14/25
 * Project: url-shortener-backend
 */
@Service
public class IdGenerationService {
    @Autowired
    private UrlMappingRepository urlMappingRepository;

    public String generateUniqueId(long value) {
        String shortCode = Base62Util.encode(value);
        long currentValue = value;
        //check if any record exists in the db with the generated short code
        while(urlMappingRepository.findByShortCode(shortCode).isPresent()) {
            currentValue++;
            shortCode = Base62Util.encode(currentValue);
        }
        return shortCode;
    }
}
