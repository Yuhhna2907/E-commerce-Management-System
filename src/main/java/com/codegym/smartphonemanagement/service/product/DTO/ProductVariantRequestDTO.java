package com.codegym.smartphonemanagement.service.product.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductVariantRequestDTO {
    @NotNull(message = "ProductId không được null")
    private Long productId;

    @NotBlank(message = "SKU không được trống")
    private String sku;

    @NotBlank(message = "Tên biến thể không được trống")
    private String variantName;

    private String color;
    private String storage;
    private String ram;

    @NotNull(message = "Giá nhập không được null")
    @Positive(message = "Giá nhập phải > 0")
    private BigDecimal costPrice;

    @NotNull(message = "Giá bán không được null")
    @Positive(message = "Giá bán phải > 0")
    private BigDecimal salePrice;

    @Min(value = 0, message = "Số lượng tồn kho phải >= 0")
    private Integer stockQuantity;

    private Boolean active = true;
}
