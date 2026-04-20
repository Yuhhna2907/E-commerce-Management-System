package com.codegym.smartphonemanagement.scheduler;

import com.codegym.smartphonemanagement.service.security.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for security maintenance operations
 * Handles periodic cleanup of old login attempts and other security-related maintenance
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityMaintenanceScheduler {

    private final LoginAttemptService loginAttemptService;

    /**
     * Clean up old login attempts daily at midnight
     * Removes login attempt records older than 90 days to prevent database bloat
     * 
     * Cron expression: "0 0 0 * * ?" means:
     * - Second: 0
     * - Minute: 0
     * - Hour: 0 (midnight)
     * - Day of month: * (every day)
     * - Month: * (every month)
     * - Day of week: ? (any day)
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanupOldLoginAttempts() {
        log.info("Starting scheduled cleanup of old login attempts");
        try {
            loginAttemptService.cleanupOldAttempts();
            log.info("Successfully completed cleanup of old login attempts");
        } catch (Exception e) {
            log.error("Error during login attempts cleanup", e);
        }
    }
}
