package com.codegym.smartphonemanagement.service.product.DTO;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDTO {
    private Long id;

    private String name;

    private String brand;

    private String description;

    private BigDecimal price;

    private Integer stock;

    private BigDecimal discountPrice;

    private String discountLabel;

    private Integer sold;

    private Integer totalQuantity;

    private String imageUrl;

    private Long categoryId;

    private String categoryName;

    private Boolean active;

    private Double averageRating;

    private Integer totalReviews;

    private List<ProductVariantResponseDTO> variants;

    private ProductSpecificationDTO specification;
}
