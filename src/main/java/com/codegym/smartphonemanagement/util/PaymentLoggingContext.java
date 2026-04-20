package com.codegym.smartphonemanagement.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Utility class for structured logging in payment operations
 * Provides consistent logging context and performance metrics
 */
@Slf4j
public class PaymentLoggingContext {

    // MDC keys for structured logging
    private static final String ORDER_ID_KEY = "orderId";
    private static final String AMOUNT_KEY = "amount";
    private static final String OPERATION_TYPE_KEY = "operationType";
    private static final String TRANSACTION_NO_KEY = "transactionNo";
    private static final String REQUEST_ID_KEY = "requestId";
    private static final String SOURCE_IP_KEY = "sourceIp";

    /**
     * Set payment transaction context for logging
     */
    public static void setTransactionContext(Long orderId, BigDecimal amount, String operationType) {
        MDC.put(ORDER_ID_KEY, String.valueOf(orderId));
        MDC.put(AMOUNT_KEY, amount != null ? amount.toString() : "null");
        MDC.put(OPERATION_TYPE_KEY, operationType);
    }

    /**
     * Set VNPay transaction number in context
     */
    public static void setVNPayTransactionNo(String transactionNo) {
        if (transactionNo != null) {
            MDC.put(TRANSACTION_NO_KEY, transactionNo);
        }
    }

    /**
     * Set refund request ID in context
     */
    public static void setRefundRequestId(String requestId) {
        if (requestId != null) {
            MDC.put(REQUEST_ID_KEY, requestId);
        }
    }

    /**
     * Set source IP for webhook processing
     */
    public static void setSourceIp(String sourceIp) {
        if (sourceIp != null) {
            MDC.put(SOURCE_IP_KEY, sourceIp);
        }
    }

    /**
     * Clear all payment context from MDC
     */
    public static void clearContext() {
        MDC.remove(ORDER_ID_KEY);
        MDC.remove(AMOUNT_KEY);
        MDC.remove(OPERATION_TYPE_KEY);
        MDC.remove(TRANSACTION_NO_KEY);
        MDC.remove(REQUEST_ID_KEY);
        MDC.remove(SOURCE_IP_KEY);
    }

    /**
     * Log payment operation start with performance tracking
     */
    public static long logOperationStart(String operation, Long orderId, BigDecimal amount) {
        setTransactionContext(orderId, amount, operation);
        long startTime = System.currentTimeMillis();
        
        log.info("Payment operation started: operation={}, orderId={}, amount={}, timestamp={}", 
                operation, orderId, amount, Instant.now());
        
        return startTime;
    }

    /**
     * Log payment operation completion with duration
     */
    public static void logOperationComplete(String operation, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        
        log.info("Payment operation completed: operation={}, duration={}ms, timestamp={}", 
                operation, duration, Instant.now());
        
        // Log performance warning if operation takes too long
        if (duration > 5000) { // 5 seconds
            log.warn("Slow payment operation detected: operation={}, duration={}ms", operation, duration);
        }
    }

    /**
     * Log payment operation error with context
     */
    public static void logOperationError(String operation, Exception error, Long orderId, BigDecimal amount) {
        setTransactionContext(orderId, amount, operation);
        
        log.error("Payment operation failed: operation={}, orderId={}, amount={}, error={}, timestamp={}", 
                operation, orderId, amount, error.getMessage(), Instant.now(), error);
    }

    /**
     * Log API call performance metrics
     */
    public static void logApiCallMetrics(String apiName, long duration, boolean success) {
        log.info("API call metrics: api={}, duration={}ms, success={}, timestamp={}", 
                apiName, duration, success, Instant.now());
        
        // Log performance warning for slow API calls
        if (duration > 10000) { // 10 seconds
            log.warn("Slow API call detected: api={}, duration={}ms", apiName, duration);
        }
    }

    /**
     * Log database operation performance
     */
    public static void logDatabaseMetrics(String operation, long duration) {
        log.debug("Database operation metrics: operation={}, duration={}ms, timestamp={}", 
                operation, duration, Instant.now());
        
        // Log performance warning for slow database operations
        if (duration > 1000) { // 1 second
            log.warn("Slow database operation detected: operation={}, duration={}ms", operation, duration);
        }
    }

    /**
     * Log security warning for signature validation failures
     */
    public static void logSecurityWarning(String event, String sourceIp, String details) {
        setSourceIp(sourceIp);
        
        log.warn("SECURITY WARNING: event={}, sourceIp={}, details={}, timestamp={}", 
                event, sourceIp, details, Instant.now());
        
        // Also log to security audit log if available
        // SecurityAuditLogger.logSecurityEvent(event, sourceIp, details);
    }

    /**
     * Log retry attempt with context
     */
    public static void logRetryAttempt(String operation, int attemptNumber, String reason) {
        log.warn("Retry attempt: operation={}, attempt={}, reason={}, timestamp={}", 
                operation, attemptNumber, reason, Instant.now());
    }

    /**
     * Log webhook processing metrics
     */
    public static void logWebhookMetrics(String vnpayTransactionNo, long processingDuration, boolean success) {
        setVNPayTransactionNo(vnpayTransactionNo);
        
        log.info("Webhook processing metrics: transactionNo={}, duration={}ms, success={}, timestamp={}", 
                vnpayTransactionNo, processingDuration, success, Instant.now());
    }
}