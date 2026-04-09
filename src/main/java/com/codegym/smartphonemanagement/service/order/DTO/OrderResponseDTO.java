package com.codegym.smartphonemanagement.service.order.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class OrderResponseDTO {
    private Long id;
    private BigDecimal totalPrice;
    private String status;
    private LocalDateTime createdAt;
    private String customerName;
    private String receiverPhone;
    private String shippingAddress;
    private String note;
    private List<OrderItemResponseDTO> items;
}