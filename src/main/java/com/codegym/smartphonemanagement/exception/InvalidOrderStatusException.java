package com.codegym.smartphonemanagement.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an order status transition is invalid.
 * This is a business exception that returns HTTP 400 (Bad Request).
 */
public class InvalidOrderStatusException extends BusinessException {
    
    private static final String ERROR_CODE = "INVALID_ORDER_STATUS";
    private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;
    
    /**
     * Constructor with current and requested status details.
     * Creates a formatted message describing the invalid status transition.
     *
     * @param currentStatus   The current status of the order
     * @param requestedStatus The requested status that cannot be applied
     */
    public InvalidOrderStatusException(String currentStatus, String requestedStatus) {
        super(
            ERROR_CODE,
            String.format("Invalid order status transition from '%s' to '%s'", 
                         currentStatus, requestedStatus),
            HTTP_STATUS
        );
        this.withAdditionalInfo("currentStatus", currentStatus)
            .withAdditionalInfo("requestedStatus", requestedStatus);
    }
    
    /**
     * Constructor with custom message.
     * Allows for flexible error messaging when status details are not available.
     *
     * @param message Custom error message
     */
    public InvalidOrderStatusException(String message) {
        super(ERROR_CODE, message, HTTP_STATUS);
    }
}
