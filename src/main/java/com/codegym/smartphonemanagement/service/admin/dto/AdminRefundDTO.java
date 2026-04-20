package com.codegym.smartphonemanagement.service.admin.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminRefundDTO {
    private Long id;
    private Long orderId;
    private String username;
    private String userEmail;
    private String reason;
    private BigDecimal totalRefundAmount;
    private String status;
    private String adminNote;
    private LocalDateTime createdAt;
}
