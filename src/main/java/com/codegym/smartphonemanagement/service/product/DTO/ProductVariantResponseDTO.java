package com.codegym.smartphonemanagement.service.product.DTO;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantResponseDTO {
    private Long variantId;
    private Long productId;
    private String sku;
    private String variantName;
    private String color;
    private String storage;
    private String ram;
    private BigDecimal costPrice;
    private BigDecimal salePrice;
    private BigDecimal discountPrice;
    private String discountLabel;
    private Integer stockQuantity;
    private Boolean active;
}
