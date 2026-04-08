package com.codegym.smartphonemanagement.service.order.DTO;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class OrderItemResponseDTO {
    private String productName;
    private String imageUrl;
    private Integer quantity;
    private BigDecimal price; // Giá tại thời điểm chốt đơn
    private BigDecimal subTotal;
}