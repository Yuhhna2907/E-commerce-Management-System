package com.codegym.smartphonemanagement.service.wallet.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WalletDTO {
    private Long walletId;
    private Long userId;
    private String username;
    private String email;
    private BigDecimal balance;
    private BigDecimal totalIn;
    private BigDecimal totalOut;
    private LocalDateTime updatedAt;
}
