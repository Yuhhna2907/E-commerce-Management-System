package com.codegym.smartphonemanagement.scheduler;

import com.codegym.smartphonemanagement.service.review.ReviewImageServiceEnhanced;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for image cleanup and maintenance
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ImageCleanupScheduler {
    
    private final ReviewImageServiceEnhanced reviewImageService;
    
    /**
     * Cleanup orphaned files daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOrphanedFiles() {
        log.info("Starting scheduled cleanup of orphaned image files");
        
        try {
            reviewImageService.cleanupOrphanedFiles();
            log.info("Completed scheduled cleanup of orphaned image files");
        } catch (Exception e) {
            log.error("Error during scheduled cleanup: {}", e.getMessage(), e);
        }
    }
}