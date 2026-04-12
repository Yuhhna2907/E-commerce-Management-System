package com.codegym.smartphonemanagement.service.order.DTO;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderHistoryDTO {
    private String statusFrom;
    private String statusTo;
    private String updatedBy;
    private String reason;
    private LocalDateTime createdAt;
}
