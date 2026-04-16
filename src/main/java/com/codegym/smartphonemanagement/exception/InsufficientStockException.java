package com.codegym.smartphonemanagement.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when there is insufficient stock to fulfill a request.
 * This is a business exception that returns HTTP 400 (Bad Request).
 */
public class InsufficientStockException extends BusinessException {
    
    private static final String ERROR_CODE = "INSUFFICIENT_STOCK";
    private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;
    
    /**
     * Constructor with product details.
     * Creates a formatted message with product name, requested quantity, and available quantity.
     *
     * @param productName The name of the product
     * @param requested   The requested quantity
     * @param available   The available quantity
     */
    public InsufficientStockException(String productName, int requested, int available) {
        super(
            ERROR_CODE,
            String.format("Insufficient stock for product '%s'. Requested: %d, Available: %d", 
                         productName, requested, available),
            HTTP_STATUS
        );
        this.withAdditionalInfo("productName", productName)
            .withAdditionalInfo("requestedQuantity", requested)
            .withAdditionalInfo("availableQuantity", available);
    }
    
    /**
     * Constructor with custom message.
     * Allows for flexible error messaging when product details are not available.
     *
     * @param message Custom error message
     */
    public InsufficientStockException(String message) {
        super(ERROR_CODE, message, HTTP_STATUS);
    }
}
