package com.codegym.smartphonemanagement.model.dto.payment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for VNPay refund API request
 * Represents the request structure for VNPay refund operations
 */
@Data
@Builder
public class VNPayRefundRequest {
    
    private String vnp_RequestId;      // Unique refund request ID
    private String vnp_Version;        // API version (2.1.0)
    private String vnp_Command;        // "refund"
    private String vnp_TmnCode;        // Terminal code
    private String vnp_TransactionType; // "02" for full refund, "03" for partial
    private String vnp_TxnRef;         // Original order ID
    private BigDecimal vnp_Amount;     // Refund amount * 100
    private String vnp_OrderInfo;      // Refund description
    private String vnp_TransactionNo;  // Original VNPay transaction number
    private String vnp_TransactionDate; // Original transaction date (yyyyMMddHHmmss)
    private String vnp_CreateBy;       // User who initiated refund
    private String vnp_CreateDate;     // Refund request date (yyyyMMddHHmmss)
    private String vnp_IpAddr;         // IP address of refund initiator
    private String vnp_SecureHash;     // HMAC-SHA512 signature
}