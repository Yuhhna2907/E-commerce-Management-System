package com.codegym.smartphonemanagement.scheduler;

import com.codegym.smartphonemanagement.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for token cleanup operations
 * Handles periodic cleanup of expired and used password reset tokens
 * Requirements: 5.12
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupScheduler {

    private final PasswordResetService passwordResetService;

    /**
     * Clean up expired and used password reset tokens every hour
     * Removes tokens that are expired or have been used for more than 24 hours
     * 
     * Cron expression: "0 0 * * * *" means:
     * - Second: 0
     * - Minute: 0
     * - Hour: * (every hour)
     * - Day of month: * (every day)
     * - Month: * (every month)
     * - Day of week: * (every day)
     * 
     * Requirements: 5.12 - Tự động xóa các Token_Reset đã hết hạn hoặc đã sử dụng sau 24 giờ
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredTokens() {
        log.info("Starting scheduled cleanup of expired password reset tokens");
        try {
            passwordResetService.cleanupExpiredTokens();
            log.info("Successfully completed cleanup of expired password reset tokens");
        } catch (Exception e) {
            log.error("Error during password reset tokens cleanup", e);
        }
    }
}
