package com.skybooker.flight.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    // FlightServiceImpl isko inject karega seat-service call ke liye
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
