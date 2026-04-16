package com.codegym.smartphonemanagement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response structure for API and web error handling.
 * Used by GlobalExceptionHandler to provide consistent error responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    /**
     * Timestamp when the error occurred
     */
    private LocalDateTime timestamp;
    
    /**
     * HTTP status code (e.g., 400, 404, 500)
     */
    private int status;
    
    /**
     * HTTP status reason phrase (e.g., "Bad Request", "Not Found")
     */
    private String error;
    
    /**
     * Human-readable error message
     */
    private String message;
    
    /**
     * Request path where the error occurred
     */
    private String path;
    
    /**
     * Unique trace ID for correlating logs and debugging
     */
    private String traceId;
    
    /**
     * Field-level validation errors (field name -> error message)
     * Used for validation exceptions
     */
    private Map<String, String> fieldErrors;
    
    /**
     * Additional contextual information about the error
     * Can include entity IDs, requested values, etc.
     */
    private Map<String, Object> additionalInfo;
}
