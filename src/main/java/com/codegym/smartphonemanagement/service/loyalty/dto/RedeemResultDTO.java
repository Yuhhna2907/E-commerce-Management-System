package com.codegym.smartphonemanagement.service.loyalty.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedeemResultDTO {
    private String couponCode;
    private BigDecimal discountValue;   // Giá trị tiền quy đổi (VD: 50.000đ)
    private Integer pointsUsed;
    private Integer remainingPoints;
    private String message;
}
