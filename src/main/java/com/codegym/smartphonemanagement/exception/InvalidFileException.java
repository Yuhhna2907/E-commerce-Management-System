package com.codegym.smartphonemanagement.exception;

/**
 * Exception thrown when a file upload fails validation
 */
public class InvalidFileException extends RuntimeException {
    
    public InvalidFileException(String message) {
        super(message);
    }
    
    public InvalidFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
