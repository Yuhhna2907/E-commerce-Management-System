package com.codegym.smartphonemanagement.service.wallet.dto;

import com.codegym.smartphonemanagement.model.WithdrawalStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WithdrawalRequestDTO {
    private Long id;
    private Long userId;
    private String username;
    private String email;
    private BigDecimal amount;
    private String bankName;
    private String bankAccount;
    private String bankHolder;
    private WithdrawalStatus status;
    private String userNote;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
