package com.codegym.smartphonemanagement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Product Engagement Analytics
 * Represents products with customer engagement metrics (wishlist + compare)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductEngagementDTO {
    
    private Long productId;
    private String productName;
    private String imageUrl;
    private String categoryName;
    private Integer wishlistCount;
    private Integer compareCount;
    private Integer engagementScore;
    private LocalDateTime lastUpdated;
}
