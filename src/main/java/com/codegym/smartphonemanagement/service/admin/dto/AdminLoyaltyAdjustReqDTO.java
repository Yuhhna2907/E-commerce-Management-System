package com.codegym.smartphonemanagement.service.admin.dto;

import lombok.Data;

@Data
public class AdminLoyaltyAdjustReqDTO {
    private int points; // can be positive (add) or negative (subtract)
    private String reason; // reason for audit
}
