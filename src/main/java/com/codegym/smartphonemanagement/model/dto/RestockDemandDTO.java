package com.codegym.smartphonemanagement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Restock Demand Analytics
 * Represents out-of-stock products with customer demand metrics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestockDemandDTO {
    
    private Long productId;
    private String productName;
    private String imageUrl;
    private String categoryName;
    private String stockStatus;
    private Integer notificationCount;
    private LocalDateTime lastOutOfStockDate;
}
