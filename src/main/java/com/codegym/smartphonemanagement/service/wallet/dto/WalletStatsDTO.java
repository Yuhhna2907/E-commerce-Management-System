package com.codegym.smartphonemanagement.service.wallet.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WalletStatsDTO {
    private BigDecimal totalBalanceInCirculation;
    private long walletsWithBalance;
    private long pendingWithdrawals;
    private BigDecimal refundedThisMonth;
    private BigDecimal withdrawnThisMonth;
}
