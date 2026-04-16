package com.codegym.smartphonemanagement.service.loyalty.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyAccountDTO {
    private Integer totalPoints;
    private Integer lifetimePoints;
    private BigDecimal estimatedValue; // totalPoints / 10 * 1000
    private LocalDateTime updatedAt;
}
