package com.codegym.smartphonemanagement.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Configuration for retry logic
 * Used for email sending with exponential backoff
 */
@Configuration
@EnableRetry
public class RetryConfig {
    // Retry is enabled via @EnableRetry annotation
    // Individual methods will use @Retryable annotation with specific retry policies
}
