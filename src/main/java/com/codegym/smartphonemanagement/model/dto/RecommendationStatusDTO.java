package com.codegym.smartphonemanagement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Recommendation Engine Status
 * Represents the current state of the recommendation system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationStatusDTO {
    
    private LocalDateTime lastRebuildTimestamp;
    private Integer totalRecommendationsCount;
    private String engineHealthStatus;
    private Boolean isRebuilding;
}
