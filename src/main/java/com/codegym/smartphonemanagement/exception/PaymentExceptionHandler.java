package com.codegym.smartphonemanagement.exception;

import com.codegym.smartphonemanagement.util.PaymentLoggingContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for payment-related operations
 * Provides consistent error responses and logging for payment exceptions
 */
@ControllerAdvice
@Slf4j
public class PaymentExceptionHandler {

    /**
     * Handle PaymentException with appropriate HTTP status and user-friendly message
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentException(
            PaymentException ex, WebRequest request) {
        
        log.error("Payment exception occurred: message={}, path={}, timestamp={}", 
                ex.getMessage(), request.getDescription(false), Instant.now(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Payment Error");
        errorResponse.put("message", getUserFriendlyMessage(ex.getMessage()));
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("path", request.getDescription(false));
        
        // Determine HTTP status based on exception type
        HttpStatus status = determineHttpStatus(ex);
        
        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * Handle signature validation failures (security-related)
     */
    @ExceptionHandler(SignatureValidationException.class)
    public ResponseEntity<Map<String, Object>> handleSignatureValidationException(
            SignatureValidationException ex, WebRequest request) {
        
        // Log security warning
        PaymentLoggingContext.logSecurityWarning(
            "SIGNATURE_VALIDATION_FAILED", 
            extractClientIp(request), 
            ex.getMessage()
        );
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Security Error");
        errorResponse.put("message", "Invalid request signature");
        errorResponse.put("timestamp", Instant.now().toString());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle retry exhaustion exceptions
     */
    @ExceptionHandler(RetryExhaustedException.class)
    public ResponseEntity<Map<String, Object>> handleRetryExhaustedException(
            RetryExhaustedException ex, WebRequest request) {
        
        log.error("Retry attempts exhausted: operation={}, attempts={}, finalError={}, timestamp={}", 
                ex.getOperation(), ex.getAttempts(), ex.getFinalError(), Instant.now(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Service Unavailable");
        errorResponse.put("message", "Payment service is temporarily unavailable. Please try again later.");
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("retryAfter", "60"); // Suggest retry after 60 seconds
        
        return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * Handle webhook processing exceptions
     */
    @ExceptionHandler(WebhookProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleWebhookProcessingException(
            WebhookProcessingException ex, WebRequest request) {
        
        log.error("Webhook processing failed: transactionNo={}, error={}, timestamp={}", 
                ex.getTransactionNo(), ex.getMessage(), Instant.now(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("RspCode", "99");
        errorResponse.put("Message", "Processing Failed");
        
        // Always return HTTP 200 for webhooks to prevent VNPay retries
        return new ResponseEntity<>(errorResponse, HttpStatus.OK);
    }

    /**
     * Handle refund operation exceptions
     */
    @ExceptionHandler(RefundException.class)
    public ResponseEntity<Map<String, Object>> handleRefundException(
            RefundException ex, WebRequest request) {
        
        log.error("Refund operation failed: orderId={}, refundRequestId={}, error={}, timestamp={}", 
                ex.getOrderId(), ex.getRefundRequestId(), ex.getMessage(), Instant.now(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Refund Error");
        errorResponse.put("message", getUserFriendlyRefundMessage(ex.getMessage()));
        errorResponse.put("orderId", ex.getOrderId());
        errorResponse.put("refundRequestId", ex.getRefundRequestId());
        errorResponse.put("timestamp", Instant.now().toString());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle general exceptions in payment context
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(
            Exception ex, WebRequest request) {
        
        // Check if this is in payment context
        if (isPaymentContext(request)) {
            log.error("Unexpected error in payment operation: error={}, path={}, timestamp={}", 
                    ex.getMessage(), request.getDescription(false), Instant.now(), ex);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal Error");
            errorResponse.put("message", "An unexpected error occurred. Please try again or contact support.");
            errorResponse.put("timestamp", Instant.now().toString());
            
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        // Let other exception handlers handle non-payment exceptions
        throw new RuntimeException(ex);
    }

    /**
     * Determine HTTP status based on PaymentException type and message
     */
    private HttpStatus determineHttpStatus(PaymentException ex) {
        String message = ex.getMessage().toLowerCase();
        
        if (message.contains("not found") || message.contains("invalid order")) {
            return HttpStatus.NOT_FOUND;
        }
        if (message.contains("invalid amount") || message.contains("validation")) {
            return HttpStatus.BAD_REQUEST;
        }
        if (message.contains("timeout") || message.contains("unavailable")) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        if (message.contains("unauthorized") || message.contains("permission")) {
            return HttpStatus.UNAUTHORIZED;
        }
        
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * Convert technical error messages to user-friendly messages
     */
    private String getUserFriendlyMessage(String technicalMessage) {
        if (technicalMessage == null) {
            return "An error occurred during payment processing";
        }
        
        String lower = technicalMessage.toLowerCase();
        
        if (lower.contains("timeout")) {
            return "Payment service is temporarily slow. Please try again.";
        }
        if (lower.contains("connection") || lower.contains("network")) {
            return "Network connection issue. Please check your connection and try again.";
        }
        if (lower.contains("invalid signature")) {
            return "Payment verification failed. Please try again.";
        }
        if (lower.contains("order not found")) {
            return "Order not found. Please verify the order details.";
        }
        if (lower.contains("invalid amount")) {
            return "Invalid payment amount. Please check the amount and try again.";
        }
        
        return "Payment processing failed. Please try again or contact support.";
    }

    /**
     * Convert technical refund error messages to user-friendly messages
     */
    private String getUserFriendlyRefundMessage(String technicalMessage) {
        if (technicalMessage == null) {
            return "An error occurred during refund processing";
        }
        
        String lower = technicalMessage.toLowerCase();
        
        if (lower.contains("already refunded")) {
            return "This order has already been refunded.";
        }
        if (lower.contains("amount exceeds")) {
            return "Refund amount exceeds the original payment amount.";
        }
        if (lower.contains("not eligible")) {
            return "This order is not eligible for refund.";
        }
        if (lower.contains("timeout")) {
            return "Refund service is temporarily slow. Please try again.";
        }
        
        return "Refund processing failed. Please try again or contact support.";
    }

    /**
     * Check if the request is in payment context
     */
    private boolean isPaymentContext(WebRequest request) {
        String path = request.getDescription(false);
        return path.contains("/payment") || path.contains("/vnpay") || path.contains("/refund");
    }

    /**
     * Extract client IP from request
     */
    private String extractClientIp(WebRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return "unknown";
    }
}