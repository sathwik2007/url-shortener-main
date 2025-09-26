package com.pm.urlshortenerbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class UrlShortenerBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerBackendApplication.class, args);
    }

}
