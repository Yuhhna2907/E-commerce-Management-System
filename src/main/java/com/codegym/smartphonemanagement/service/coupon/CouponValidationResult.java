package com.codegym.smartphonemanagement.service.coupon;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CouponValidationResult {
    private boolean valid;
    private String message;
    private BigDecimal discountAmount;
}
