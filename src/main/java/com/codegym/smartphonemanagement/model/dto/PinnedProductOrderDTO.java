package com.codegym.smartphonemanagement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Pinned Product Reordering
 * Used to update the display order of pinned products
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinnedProductOrderDTO {
    
    private Long pinnedProductId;
    private Integer displayOrder;
}
