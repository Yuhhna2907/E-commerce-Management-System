package com.codegym.smartphonemanagement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Recommendation Matrix Rebuild Result
 * Represents the outcome of a recommendation rebuild operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RebuildResultDTO {
    
    private Boolean success;
    private String message;
    private Integer totalProducts;
    private Integer successCount;
    private Integer skippedCount;
    private Integer failedCount;
    private Long durationMs;
    private LocalDateTime completedAt;
}
