package com.codegym.smartphonemanagement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandStatDTO {
    private String brandName;
    private Long productCount;
    private Double marketShare; // Percentage of total products
    
    public BrandStatDTO(String brandName, Long productCount) {
        this.brandName = brandName;
        this.productCount = productCount;
        this.marketShare = 0.0; // Will be calculated later
    }
}