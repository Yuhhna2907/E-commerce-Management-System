package com.codegym.smartphonemanagement.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested resource cannot be found.
 * Extends BusinessException for consistent error handling.
 * Returns HTTP 404 (Not Found) status code.
 */
public class ResourceNotFoundException extends BusinessException {
    
    private static final String ERROR_CODE = "RESOURCE_NOT_FOUND";
    private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;
    
    /**
     * Constructor with error message.
     *
     * @param message Human-readable error message describing the missing resource
     */
    public ResourceNotFoundException(String message) {
        super(ERROR_CODE, message, HTTP_STATUS);
    }
    
    /**
     * Constructor with error message and cause.
     *
     * @param message Human-readable error message describing the missing resource
     * @param cause   The underlying cause of this exception
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(ERROR_CODE, message, HTTP_STATUS, cause);
    }
}
