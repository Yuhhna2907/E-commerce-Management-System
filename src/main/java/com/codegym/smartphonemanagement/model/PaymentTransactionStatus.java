package com.codegym.smartphonemanagement.model;

/**
 * Enum representing the status of a payment transaction.
 * Used to track the lifecycle of payment transactions from initiation to completion or failure.
 */
public enum PaymentTransactionStatus {
    /**
     * Payment URL created, awaiting user action
     */
    INITIATED,
    
    /**
     * Payment successful (vnp_ResponseCode = 00)
     */
    COMPLETED,
    
    /**
     * Payment failed or cancelled
     */
    FAILED
}
