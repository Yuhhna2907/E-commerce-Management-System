package com.codegym.smartphonemanagement.service.order.DTO;

import com.codegym.smartphonemanagement.model.OrderStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class OrderResponseDTO {
    private Long id;
    private BigDecimal totalPrice;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private String customerName;
    private List<OrderItemResponseDTO> items;
}