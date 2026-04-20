package com.codegym.smartphonemanagement.exception;

/**
 * Exception thrown when signature validation fails
 * Indicates potential security issues or tampered requests
 */
public class SignatureValidationException extends PaymentException {
    
    public SignatureValidationException(String message) {
        super(message);
    }
    
    public SignatureValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}