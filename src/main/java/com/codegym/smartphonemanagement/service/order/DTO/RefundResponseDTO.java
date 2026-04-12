package com.codegym.smartphonemanagement.service.order.DTO;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefundResponseDTO {
    private Long id;
    private Long orderId;
    private String reason;
    private BigDecimal totalRefundAmount;
    private String status;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // Thông tin user (cho Admin view)
    private String userName;
    private List<RefundItemDetailDTO> items;
}
