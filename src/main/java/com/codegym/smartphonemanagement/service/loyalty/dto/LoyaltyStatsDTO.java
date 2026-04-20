package com.codegym.smartphonemanagement.service.loyalty.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyStatsDTO {
    private long totalAccounts;
    private long totalPointsInCirculation;
    private long totalLifetimePoints;
    private long totalPointsRedeemed;
    private BigDecimal circulationValue; // totalPointsInCirculation quy ra VND
}
