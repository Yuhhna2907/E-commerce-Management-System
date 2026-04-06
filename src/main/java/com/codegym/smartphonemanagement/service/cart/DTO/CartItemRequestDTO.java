package com.codegym.smartphonemanagement.service.cart.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartItemRequestDTO {

    @NotNull
    private Long productId;

    @Min(1)
    private Integer quantity;
}