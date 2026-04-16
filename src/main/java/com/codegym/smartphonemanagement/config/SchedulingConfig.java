package com.codegym.smartphonemanagement.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for scheduled tasks
 * Used for recommendation engine rebuild and other periodic tasks
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Scheduling is enabled via @EnableScheduling annotation
    // Individual scheduled tasks will use @Scheduled annotation
}
