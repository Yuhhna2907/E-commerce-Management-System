package com.codegym.smartphonemanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for RestTemplate beans
 * Provides HTTP client configuration for external API calls
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Creates RestTemplate bean with timeout configuration
     * Used for VNPay API calls and other external services
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        // Set timeouts for VNPay API calls
        factory.setConnectTimeout(5000);  // 5 seconds connection timeout
        factory.setReadTimeout(30000);    // 30 seconds read timeout
        
        return new RestTemplate(factory);
    }
}