package com.codegym.smartphonemanagement.model.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedForLaterDTO {
    
    private Long id;
    private Long userId;
    private Long productId;
    private Long variantId;
    private Integer quantity;
    private LocalDateTime savedAt;
    private String note;
    
    // Product details
    private String productName;
    private String productImage;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String discountLabel;
    private Integer stock;
    private String brand;
    
    // Variant details (if applicable)
    private String color;
    private String storage;
    private String ram;
}
