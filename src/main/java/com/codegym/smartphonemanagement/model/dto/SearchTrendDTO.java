package com.codegym.smartphonemanagement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Search Trend Analytics
 * Represents search keywords with frequency and conversion metrics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchTrendDTO {
    
    private String searchKeyword;
    private Integer searchCount;
    private Integer conversionCount;
    private BigDecimal conversionRate;
    private String trendIndicator;
    private LocalDateTime firstSearchedDate;
    private LocalDateTime lastSearchedDate;
}
