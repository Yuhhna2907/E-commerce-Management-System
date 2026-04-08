package com.codegym.smartphonemanagement.service.product.DTO;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductRequestDTO {
    @NotBlank(message = "Tên sản phẩm không được trống")
    private String name;

    @NotBlank(message = "Hãng không được trống")
    private String brand;

    private String color;

    private String description;

    @NotNull(message = "Giá không được null")
    @Positive(message = "Giá phải lớn hơn 0")
    private BigDecimal price;

    @NotNull(message = "Số lượng không được null")
    @Min(value = 0, message = "Số lượng phải >= 0")
    private Integer stock;

    @NotNull(message = "Category không được null")
    private Long categoryId;

    private String imageUrl;
}
