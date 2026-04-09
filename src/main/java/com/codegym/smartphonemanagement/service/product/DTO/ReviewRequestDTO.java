package com.codegym.smartphonemanagement.service.product.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequestDTO {
    @NotNull(message = "Product ID không được null")
    private Long productId;

    @NotNull(message = "Rating không được null")
    @Min(value = 1, message = "Rating tối thiểu là 1")
    @Max(value = 5, message = "Rating tối đa là 5")
    private Integer rating;

    @Size(max = 1000, message = "Comment tối đa 1000 ký tự")
    private String comment;
}
