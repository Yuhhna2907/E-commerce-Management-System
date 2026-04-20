package com.codegym.smartphonemanagement.service.security;

import com.codegym.smartphonemanagement.config.ImageProcessingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple rate limiting service for image uploads
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitingService {
    
    private final ImageProcessingConfig config;
    private final ConcurrentHashMap<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();
    
    /**
     * Check if upload is allowed for the given identifier (IP or user ID)
     */
    public boolean isUploadAllowed(String identifier) {
        RateLimitInfo info = rateLimitMap.computeIfAbsent(identifier, k -> new RateLimitInfo());
        
        LocalDateTime now = LocalDateTime.now();
        
        // Clean up old entries
        cleanupOldEntries(info, now);
        
        // Check per minute limit
        long uploadsInLastMinute = info.getUploadsInWindow(now.minusMinutes(1));
        if (uploadsInLastMinute >= config.getMaxUploadsPerMinute()) {
            log.warn("Rate limit exceeded for identifier: {} (per minute)", identifier);
            return false;
        }
        
        // Check per hour limit
        long uploadsInLastHour = info.getUploadsInWindow(now.minusHours(1));
        if (uploadsInLastHour >= config.getMaxUploadsPerHour()) {
            log.warn("Rate limit exceeded for identifier: {} (per hour)", identifier);
            return false;
        }
        
        // Record this upload
        info.recordUpload(now);
        
        return true;
    }
    
    /**
     * Get remaining tokens for the identifier (approximate)
     */
    public long getRemainingTokens(String identifier) {
        RateLimitInfo info = rateLimitMap.get(identifier);
        if (info == null) {
            return config.getMaxUploadsPerMinute();
        }
        
        LocalDateTime now = LocalDateTime.now();
        long uploadsInLastMinute = info.getUploadsInWindow(now.minusMinutes(1));
        
        return Math.max(0, config.getMaxUploadsPerMinute() - uploadsInLastMinute);
    }
    
    /**
     * Clear rate limit for identifier (admin function)
     */
    public void clearRateLimit(String identifier) {
        rateLimitMap.remove(identifier);
        log.info("Cleared rate limit for identifier: {}", identifier);
    }
    
    /**
     * Clear all rate limits (admin function)
     */
    public void clearAllRateLimits() {
        rateLimitMap.clear();
        log.info("Cleared all rate limits");
    }
    
    /**
     * Clean up old entries to prevent memory leaks
     */
    private void cleanupOldEntries(RateLimitInfo info, LocalDateTime now) {
        info.cleanupOldUploads(now.minusHours(1));
    }
    
    /**
     * Rate limit information for a single identifier
     */
    private static class RateLimitInfo {
        private final ConcurrentHashMap<LocalDateTime, AtomicInteger> uploadTimes = new ConcurrentHashMap<>();
        
        public void recordUpload(LocalDateTime time) {
            // Round to nearest second for grouping
            LocalDateTime roundedTime = time.truncatedTo(ChronoUnit.SECONDS);
            uploadTimes.computeIfAbsent(roundedTime, k -> new AtomicInteger(0)).incrementAndGet();
        }
        
        public long getUploadsInWindow(LocalDateTime since) {
            return uploadTimes.entrySet().stream()
                .filter(entry -> entry.getKey().isAfter(since))
                .mapToLong(entry -> entry.getValue().get())
                .sum();
        }
        
        public void cleanupOldUploads(LocalDateTime cutoff) {
            uploadTimes.entrySet().removeIf(entry -> entry.getKey().isBefore(cutoff));
        }
    }
}