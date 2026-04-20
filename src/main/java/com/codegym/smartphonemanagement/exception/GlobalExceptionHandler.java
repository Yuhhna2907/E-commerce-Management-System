package com.codegym.smartphonemanagement.exception;

import com.codegym.smartphonemanagement.model.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for the E-commerce Management System
 * Handles all exceptions centrally and returns consistent error responses
 * Requirements: 3.1
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final HttpServletRequest request;

    /**
     * Constructor injection for HttpServletRequest
     * Allows capturing request path information for error responses
     */
    public GlobalExceptionHandler(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Generate a unique trace ID for exception tracking
     * @return UUID string for correlation and debugging
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Handle BusinessException and all its subtypes.
     * Extracts error code, message, and HTTP status from the exception,
     * builds a structured ErrorResponse, and logs at INFO level.
     * 
     * Requirements: 3.2, 3.5, 3.6, 6.4
     * 
     * @param ex      The BusinessException that was thrown
     * @param request The HTTP request that triggered the exception
     * @return ResponseEntity containing ErrorResponse with appropriate HTTP status
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, 
            HttpServletRequest request) {
        
        // Generate unique trace ID for this exception
        String traceId = generateTraceId();
        
        // Log at INFO level for business exceptions (not system errors)
        log.info("Business exception occurred - Error Code: {}, Message: {}, Path: {}, TraceId: {}", 
                ex.getErrorCode(), 
                ex.getMessage(), 
                request.getRequestURI(), 
                traceId);
        
        // Build ErrorResponse with all required fields
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getHttpStatus().value())
                .error(ex.getHttpStatus().getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(traceId)
                .additionalInfo(ex.getAdditionalInfo())
                .build();
        
        // Return ResponseEntity with appropriate HTTP status from exception
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(errorResponse);
    }

    /**
     * Handle validation exceptions (MethodArgumentNotValidException).
     * Extracts field-level validation errors from BindingResult,
     * builds a structured ErrorResponse with fieldErrors map, and logs at WARN level.
     * 
     * Requirements: 3.3, 4.3
     * 
     * @param ex      The MethodArgumentNotValidException that was thrown
     * @param request The HTTP request that triggered the exception
     * @return ResponseEntity containing ErrorResponse with HTTP 400 status
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        // Generate unique trace ID for this exception
        String traceId = generateTraceId();
        
        // Extract field errors from BindingResult
        BindingResult bindingResult = ex.getBindingResult();
        Map<String, String> fieldErrors = new HashMap<>();
        
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        
        // Log at WARN level for validation errors
        log.warn("Validation failed - Field Errors: {}, Path: {}, TraceId: {}", 
                fieldErrors, 
                request.getRequestURI(), 
                traceId);
        
        // Build ErrorResponse with fieldErrors map
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed for one or more fields")
                .path(request.getRequestURI())
                .traceId(traceId)
                .fieldErrors(fieldErrors)
                .build();
        
        // Return ResponseEntity with HTTP 400 status
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * Handle generic RuntimeException that are not caught by more specific handlers.
     * Logs at ERROR level with full stack trace for debugging,
     * builds a generic ErrorResponse with sanitized message to prevent information leakage,
     * and returns HTTP 500 status.
     * 
     * Requirements: 3.4, 6.2, 6.5, 8.2
     * 
     * @param ex      The RuntimeException that was thrown
     * @param request The HTTP request that triggered the exception
     * @return ResponseEntity containing ErrorResponse with HTTP 500 status
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request) {
        
        // Generate unique trace ID for this exception
        String traceId = generateTraceId();
        
        // Log at ERROR level with full stack trace for debugging
        log.error("Unexpected runtime exception occurred - Message: {}, Path: {}, TraceId: {}", 
                ex.getMessage(), 
                request.getRequestURI(), 
                traceId, 
                ex);
        
        // Sanitize error message to prevent information leakage
        // Use generic message instead of exposing internal details
        String sanitizedMessage = "An unexpected error occurred. Please contact support with trace ID: " + traceId;
        
        // Build generic ErrorResponse
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(sanitizedMessage)
                .path(request.getRequestURI())
                .traceId(traceId)
                .build();
        
        // Return ResponseEntity with HTTP 500 status
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    /**
     * Handle generic Exception (checked exceptions and other exceptions not caught by specific handlers).
     * Logs at ERROR level with full stack trace for debugging,
     * builds a generic ErrorResponse with user-friendly message,
     * excludes sensitive information from response, and returns HTTP 500 status.
     * 
     * This is the catch-all handler for any exception not handled by more specific handlers.
     * It ensures that no exception goes unhandled and all errors return a consistent response format.
     * 
     * Requirements: 3.4, 6.2, 6.3, 6.5
     * 
     * @param ex      The Exception that was thrown
     * @param request The HTTP request that triggered the exception
     * @return ResponseEntity containing ErrorResponse with HTTP 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        
        // Generate unique trace ID for this exception
        String traceId = generateTraceId();
        
        // Log at ERROR level with full stack trace for debugging
        // Include exception type to help identify the root cause
        log.error("Unexpected exception occurred - Type: {}, Message: {}, Path: {}, TraceId: {}", 
                ex.getClass().getName(),
                ex.getMessage(), 
                request.getRequestURI(), 
                traceId, 
                ex);
        
        // Build generic ErrorResponse with user-friendly message
        // Exclude sensitive information from response (no stack trace, no internal details)
        String userFriendlyMessage = "We're sorry, but something went wrong. Our team has been notified. Please try again later or contact support with trace ID: " + traceId;
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(userFriendlyMessage)
                .path(request.getRequestURI())
                .traceId(traceId)
                .build();
        
        // Return ResponseEntity with HTTP 500 status
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}
