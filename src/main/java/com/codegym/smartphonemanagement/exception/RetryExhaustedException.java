package com.codegym.smartphonemanagement.exception;

import lombok.Getter;

/**
 * Exception thrown when all retry attempts are exhausted
 * Contains information about the failed operation and retry attempts
 */
@Getter
public class RetryExhaustedException extends PaymentException {
    
    private final String operation;
    private final int attempts;
    private final Throwable finalError;
    
    public RetryExhaustedException(String operation, int attempts, Throwable finalError) {
        super(String.format("Operation '%s' failed after %d retry attempts. Final error: %s", 
              operation, attempts, finalError.getMessage()), finalError);
        this.operation = operation;
        this.attempts = attempts;
        this.finalError = finalError;
    }
}