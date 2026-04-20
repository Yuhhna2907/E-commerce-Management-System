package com.codegym.smartphonemanagement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for tracking image upload progress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadProgress {
    private String sessionId;
    private int totalFiles;
    private int processedFiles;
    private int successfulUploads;
    private int failedUploads;
    private String currentFileName;
    private String status; // PROCESSING, COMPLETED, FAILED
    private double progressPercentage;
    private String errorMessage;
}