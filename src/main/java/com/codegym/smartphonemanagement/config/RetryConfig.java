package com.codegym.smartphonemanagement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Configuration for retry logic
 * Used for email sending and payment API calls with exponential backoff
 */
@Configuration
@EnableRetry
public class RetryConfig {
    
    @Value("${app.payment.retry.max-attempts:3}")
    private int maxAttempts;
    
    @Value("${app.payment.retry.initial-delay:1000}")
    private long initialDelay;
    
    @Value("${app.payment.retry.multiplier:2.0}")
    private double multiplier;
    
    @Value("${app.payment.retry.max-delay:10000}")
    private long maxDelay;
    
    /**
     * RetryTemplate bean for payment API calls
     * Configured with exponential backoff policy
     * 
     * @return RetryTemplate configured for payment operations
     */
    @Bean
    public RetryTemplate paymentRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Configure retry policy
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(maxAttempts);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        // Configure exponential backoff policy
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(initialDelay);
        backOffPolicy.setMultiplier(multiplier);
        backOffPolicy.setMaxInterval(maxDelay);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        return retryTemplate;
    }
}
