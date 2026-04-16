package com.codegym.smartphonemanagement.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a request is malformed or contains invalid data.
 * Extends BusinessException for consistent error handling.
 * Returns HTTP 400 (Bad Request) status code.
 */
public class BadRequestException extends BusinessException {
    
    private static final String ERROR_CODE = "BAD_REQUEST";
    private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;
    
    /**
     * Constructor with error message.
     *
     * @param message Human-readable error message describing the bad request
     */
    public BadRequestException(String message) {
        super(ERROR_CODE, message, HTTP_STATUS);
    }
    
    /**
     * Constructor with error message and cause.
     *
     * @param message Human-readable error message describing the bad request
     * @param cause   The underlying cause of this exception
     */
    public BadRequestException(String message, Throwable cause) {
        super(ERROR_CODE, message, HTTP_STATUS, cause);
    }
}
