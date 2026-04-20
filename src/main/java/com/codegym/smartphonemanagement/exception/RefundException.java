package com.codegym.smartphonemanagement.exception;

import lombok.Getter;

/**
 * Exception thrown during refund operation failures
 * Contains refund context for debugging and user feedback
 */
@Getter
public class RefundException extends PaymentException {
    
    private final Long orderId;
    private final String refundRequestId;
    
    public RefundException(String message, Long orderId, String refundRequestId) {
        super(message);
        this.orderId = orderId;
        this.refundRequestId = refundRequestId;
    }
    
    public RefundException(String message, Long orderId, String refundRequestId, Throwable cause) {
        super(message, cause);
        this.orderId = orderId;
        this.refundRequestId = refundRequestId;
    }
}