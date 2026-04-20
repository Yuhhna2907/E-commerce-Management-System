package com.codegym.smartphonemanagement.exception;

import lombok.Getter;

/**
 * Exception thrown during webhook processing failures
 * Contains transaction context for debugging
 */
@Getter
public class WebhookProcessingException extends PaymentException {
    
    private final String transactionNo;
    private final String orderId;
    
    public WebhookProcessingException(String message, String transactionNo, String orderId) {
        super(message);
        this.transactionNo = transactionNo;
        this.orderId = orderId;
    }
    
    public WebhookProcessingException(String message, String transactionNo, String orderId, Throwable cause) {
        super(message, cause);
        this.transactionNo = transactionNo;
        this.orderId = orderId;
    }
}