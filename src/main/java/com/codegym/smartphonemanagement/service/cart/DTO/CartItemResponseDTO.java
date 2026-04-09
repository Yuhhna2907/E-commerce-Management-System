package com.codegym.smartphonemanagement.service.cart.DTO;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CartItemResponseDTO {
    private Long id;
    private Long productId;
    private Long variantId;
    private String productName;
    private String variantName;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String discountLabel;
    private String imageUrl;
    private Integer quantity;
    private Integer stockQuantity;
    private BigDecimal total;
    private String brand;
    private String color;
    private String storage;
    private String ram;
    private BigDecimal subTotal;
}