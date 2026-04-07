package com.codegym.smartphonemanagement.service.product.DTO;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDTO {
    private Long id;

    private String name;

    private String brand;

    private String color;

    private String description;

    private BigDecimal price;

    private Integer stock;

    private String imageUrl;

    private Long categoryId;

    private String categoryName;

    private Boolean active;
}
