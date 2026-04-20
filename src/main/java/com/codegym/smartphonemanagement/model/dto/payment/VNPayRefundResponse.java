package com.codegym.smartphonemanagement.model.dto.payment;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for VNPay refund API response
 * Represents the response structure from VNPay refund operations
 */
@Data
public class VNPayRefundResponse {
    
    private String vnp_ResponseCode;   // "00" = success, "02" = processing, other = failed
    private String vnp_Message;        // Response message
    private String vnp_TransactionNo;  // VNPay refund transaction number
    private BigDecimal vnp_Amount;     // Refund amount
    private String vnp_BankCode;       // Bank code
    private String vnp_PayDate;        // Refund completion date
    private String vnp_SecureHash;     // Response signature
}