package com.codegym.smartphonemanagement.service.cart.DTO;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CartItemResponseDTO {
    private Long productId;
    private String productName;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String discountLabel;
    private String imageUrl;
    private Integer quantity;
    private BigDecimal total;
    private String brand;
    private String color;
    private BigDecimal subTotal;
}