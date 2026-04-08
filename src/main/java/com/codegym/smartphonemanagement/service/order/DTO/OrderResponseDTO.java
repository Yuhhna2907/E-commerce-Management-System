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
    private String status; // Duy có thể để OrderStatus hoặc String
    private LocalDateTime createdAt;

    // Các trường Duy đang thiếu dẫn đến lỗi "Cannot resolve method"
    private String customerName;
    private String receiverPhone;
    private String shippingAddress;
    private String note;

    private List<OrderItemResponseDTO> items;
}