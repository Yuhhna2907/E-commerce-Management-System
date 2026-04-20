package com.codegym.smartphonemanagement.exception;

/**
 * Exception thrown when payment processing fails
 * Used for VNPay payment gateway errors and validation failures
 */
public class PaymentException extends RuntimeException {
    
    public PaymentException(String message) {
        super(message);
    }
    
    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
