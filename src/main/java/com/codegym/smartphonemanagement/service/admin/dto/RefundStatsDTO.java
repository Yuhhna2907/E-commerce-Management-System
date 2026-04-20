package com.codegym.smartphonemanagement.service.admin.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class RefundStatsDTO {
    private long pendingCount;
    private BigDecimal pendingAmount;
    private long approvedCount;
    private BigDecimal approvedAmount;
    private long rejectedCount;

    // Date range context
    private LocalDate dateFrom;
    private LocalDate dateTo;
}
