package com.codegym.smartphonemanagement.model;

/**
 * Status enum for VNPay refund transactions
 * Represents the lifecycle of a refund request through VNPay API
 */
public enum VNPayRefundStatus {
    /**
     * Refund request submitted to VNPay, awaiting processing
     */
    PENDING,
    
    /**
     * VNPay is processing the refund (vnp_ResponseCode = 02)
     */
    PROCESSING,
    
    /**
     * Refund completed successfully (vnp_ResponseCode = 00)
     */
    COMPLETED,
    
    /**
     * Refund failed
     */
    FAILED
}
