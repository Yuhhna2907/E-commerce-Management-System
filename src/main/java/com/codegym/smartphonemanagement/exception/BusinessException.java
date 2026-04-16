package com.codegym.smartphonemanagement.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for all business exceptions in the system.
 * Provides a structured approach to exception handling with error codes,
 * HTTP status codes, and additional contextual information.
 */
public abstract class BusinessException extends RuntimeException implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;

    @Getter
    private final String errorCode;

    @Getter
    private final HttpStatus httpStatus;
    private final Map<String, Object> additionalInfo;
    
    /**
     * Constructor with error code, message, and HTTP status.
     *
     * @param errorCode  Unique error code identifying the exception type
     * @param message    Human-readable error message
     * @param httpStatus HTTP status code to be returned in the response
     */
    protected BusinessException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.additionalInfo = new HashMap<>();
    }
    
    /**
     * Constructor with error code, message, HTTP status, and cause.
     *
     * @param errorCode  Unique error code identifying the exception type
     * @param message    Human-readable error message
     * @param httpStatus HTTP status code to be returned in the response
     * @param cause      The underlying cause of this exception
     */
    protected BusinessException(String errorCode, String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.additionalInfo = new HashMap<>();
    }

    /**
     * Gets the additional information map.
     *
     * @return Map containing additional contextual information
     */
    public Map<String, Object> getAdditionalInfo() {
        return new HashMap<>(additionalInfo);
    }
    
    /**
     * Adds additional information to the exception context.
     * Supports method chaining for fluent API usage.
     *
     * @param key   The key for the additional information
     * @param value The value to associate with the key
     * @return This exception instance for method chaining
     */
    public BusinessException withAdditionalInfo(String key, Object value) {
        this.additionalInfo.put(key, value);
        return this;
    }
    
    /**
     * Adds a correlation ID to the exception context.
     * The correlation ID is stored in additionalInfo with key "correlationId".
     * Supports method chaining for fluent API usage.
     *
     * @param correlationId The correlation ID for request tracing
     * @return This exception instance for method chaining
     */
    public BusinessException withCorrelationId(String correlationId) {
        this.additionalInfo.put("correlationId", correlationId);
        return this;
    }
}
