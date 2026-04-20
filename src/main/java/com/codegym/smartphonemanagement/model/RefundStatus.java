package com.codegym.smartphonemanagement.model;

/**
 * Enum representing the status of a refund transaction.
 * Used to track the lifecycle of refund transactions from submission to completion or failure.
 */
public enum RefundStatus {
    /**
     * Refund request submitted to VNPay
     */
    PENDING,
    
    /**
     * VNPay is processing refund (vnp_ResponseCode = 02)
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
