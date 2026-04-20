package com.codegym.smartphonemanagement.service.progress;

import com.codegym.smartphonemanagement.model.dto.ImageUploadProgress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for tracking upload progress
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UploadProgressService {
    
    private final ConcurrentHashMap<String, ImageUploadProgress> progressMap = new ConcurrentHashMap<>();
    
    /**
     * Initialize progress tracking for a session
     */
    public void initializeProgress(String sessionId, int totalFiles) {
        ImageUploadProgress progress = ImageUploadProgress.builder()
            .sessionId(sessionId)
            .totalFiles(totalFiles)
            .processedFiles(0)
            .successfulUploads(0)
            .failedUploads(0)
            .status("PROCESSING")
            .progressPercentage(0.0)
            .build();
        
        progressMap.put(sessionId, progress);
        log.debug("Initialized progress tracking for session: {} with {} files", sessionId, totalFiles);
    }
    
    /**
     * Update progress for current file
     */
    public void updateProgress(String sessionId, String currentFileName) {
        ImageUploadProgress progress = progressMap.get(sessionId);
        if (progress != null) {
            progress.setCurrentFileName(currentFileName);
            progressMap.put(sessionId, progress);
        }
    }
    
    /**
     * Mark file as successfully processed
     */
    public void markFileSuccess(String sessionId) {
        ImageUploadProgress progress = progressMap.get(sessionId);
        if (progress != null) {
            progress.setProcessedFiles(progress.getProcessedFiles() + 1);
            progress.setSuccessfulUploads(progress.getSuccessfulUploads() + 1);
            updateProgressPercentage(progress);
            progressMap.put(sessionId, progress);
        }
    }
    
    /**
     * Mark file as failed
     */
    public void markFileFailure(String sessionId, String errorMessage) {
        ImageUploadProgress progress = progressMap.get(sessionId);
        if (progress != null) {
            progress.setProcessedFiles(progress.getProcessedFiles() + 1);
            progress.setFailedUploads(progress.getFailedUploads() + 1);
            progress.setErrorMessage(errorMessage);
            updateProgressPercentage(progress);
            progressMap.put(sessionId, progress);
        }
    }
    
    /**
     * Mark upload as completed
     */
    public void markCompleted(String sessionId) {
        ImageUploadProgress progress = progressMap.get(sessionId);
        if (progress != null) {
            progress.setStatus("COMPLETED");
            progress.setProgressPercentage(100.0);
            progress.setCurrentFileName(null);
            progressMap.put(sessionId, progress);
            
            log.info("Upload completed for session: {}. Success: {}, Failed: {}", 
                sessionId, progress.getSuccessfulUploads(), progress.getFailedUploads());
        }
    }
    
    /**
     * Mark upload as failed
     */
    public void markFailed(String sessionId, String errorMessage) {
        ImageUploadProgress progress = progressMap.get(sessionId);
        if (progress != null) {
            progress.setStatus("FAILED");
            progress.setErrorMessage(errorMessage);
            progressMap.put(sessionId, progress);
            
            log.error("Upload failed for session: {}. Error: {}", sessionId, errorMessage);
        }
    }
    
    /**
     * Get current progress
     */
    public ImageUploadProgress getProgress(String sessionId) {
        return progressMap.get(sessionId);
    }
    
    /**
     * Clean up progress tracking
     */
    public void cleanupProgress(String sessionId) {
        progressMap.remove(sessionId);
        log.debug("Cleaned up progress tracking for session: {}", sessionId);
    }
    
    /**
     * Update progress percentage
     */
    private void updateProgressPercentage(ImageUploadProgress progress) {
        if (progress.getTotalFiles() > 0) {
            double percentage = (double) progress.getProcessedFiles() / progress.getTotalFiles() * 100.0;
            progress.setProgressPercentage(Math.round(percentage * 100.0) / 100.0);
        }
    }
}