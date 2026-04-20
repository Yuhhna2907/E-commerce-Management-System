package com.codegym.smartphonemanagement.service.loyalty.dto;

import com.codegym.smartphonemanagement.model.MemberTier;
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

    // --- User info (for admin list) ---
    private Long userId;
    private String username;
    private String email;
    private String fullName;

    // --- Tier fields ---
    private MemberTier tier;
    private String tierLabel;       // "Vàng"
    private String tierColor;       // "#f59e0b"
    private String tierIcon;        // "bi-star-fill"
    private double bonusMultiplier; // 1.10 = +10%
    private Integer pointsToNextTier; // null nếu Kim Cương
    private int progressToNextTier;   // 0–100
}

