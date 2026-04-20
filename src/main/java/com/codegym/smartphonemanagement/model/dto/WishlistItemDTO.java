package com.codegym.smartphonemanagement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItemDTO {
    private Long wishlistId;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String discountLabel;
    private Integer stock;
    private LocalDateTime addedAt;
    private String brand;
    private Boolean inStock;
    private String category;
}
