package com.codegym.smartphonemanagement.service.payment;

import com.codegym.smartphonemanagement.model.PaymentTransactionStatus;
import com.codegym.smartphonemanagement.model.dto.payment.WebhookResponse;
import com.codegym.smartphonemanagement.service.order.user.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for processing VNPay IPN (Instant Payment Notification) webhooks
 * Handles asynchronous webhook processing with signature verification and idempotency
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IPNWebhookHandler {

    private final SignatureVerifier signatureVerifier;
    private final IdempotencyGuard idempotencyGuard;
    private final PaymentTransactionLogger transactionLogger;
    private final OrderService orderService;

    /**
     * Process VNPay IPN webhook asynchronously
     * Validates signature, checks idempotency, and updates order status
     * 
     * @param ipnParams Map of IPN parameters from VNPay
     * @param sourceIp Source IP address of the webhook request
     * @return CompletableFuture with webhook response
     */
    @Async("webhookExecutor")
    @Transactional
    public CompletableFuture<WebhookResponse> processIPN(Map<String, String> ipnParams, String sourceIp) {
        String vnpayTransactionNo = ipnParams.get("vnp_TransactionNo");
        String orderId = ipnParams.get("vnp_TxnRef");
        String responseCode = ipnParams.get("vnp_ResponseCode");
        
        log.info("Processing IPN webhook: orderId={}, vnpayTransactionNo={}, responseCode={}, sourceIp={}", 
                orderId, vnpayTransactionNo, responseCode, sourceIp);

        try {
            // Step 1: Validate signature
            if (!validateSignature(ipnParams)) {
                log.warn("IPN signature validation failed: orderId={}, sourceIp={}", orderId, sourceIp);
                return CompletableFuture.completedFuture(
                    WebhookResponse.failure("Invalid signature")
                );
            }

            // Step 2: Check idempotency
            if (checkIdempotency(vnpayTransactionNo)) {
                log.info("Duplicate IPN detected, returning success: vnpayTransactionNo={}", vnpayTransactionNo);
                return CompletableFuture.completedFuture(WebhookResponse.success());
            }

            // Step 3: Update transaction record
            PaymentTransactionStatus status = mapResponseCodeToStatus(responseCode);
            transactionLogger.updateFromCallback(vnpayTransactionNo, ipnParams);

            // Step 4: Update order status
            if (orderId != null && !orderId.isEmpty()) {
                updateOrderStatus(Long.parseLong(orderId), status);
            }

            log.info("IPN processing completed successfully: orderId={}, vnpayTransactionNo={}, status={}", 
                    orderId, vnpayTransactionNo, status);

            return CompletableFuture.completedFuture(WebhookResponse.success());

        } catch (Exception e) {
            log.error("IPN processing failed: orderId={}, vnpayTransactionNo={}, error={}", 
                     orderId, vnpayTransactionNo, e.getMessage(), e);
            
            // Log error but don't update transaction status to avoid inconsistency
            try {
                if (orderId != null && !orderId.isEmpty()) {
                    transactionLogger.logError(Long.parseLong(orderId), 
                        "IPN processing failed: " + e.getMessage());
                }
            } catch (Exception logError) {
                log.error("Failed to log IPN error: {}", logError.getMessage());
            }

            return CompletableFuture.completedFuture(
                WebhookResponse.failure("Processing failed: " + e.getMessage())
            );
        }
    }

    /**
     * Validate HMAC-SHA512 signature from VNPay
     * 
     * @param params IPN parameters including signature
     * @return true if signature is valid, false otherwise
     */
    private boolean validateSignature(Map<String, String> params) {
        try {
            String providedSignature = params.get("vnp_SecureHash");
            if (providedSignature == null || providedSignature.isEmpty()) {
                log.warn("Missing vnp_SecureHash in IPN parameters");
                return false;
            }

            // Create a copy without the signature for verification
            Map<String, String> paramsForVerification = new java.util.HashMap<>(params);
            paramsForVerification.remove("vnp_SecureHash");

            return signatureVerifier.verifyHmacSHA512(paramsForVerification, providedSignature);

        } catch (Exception e) {
            log.error("Signature validation error: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if this webhook has already been processed
     * 
     * @param vnpayTransactionNo VNPay transaction number
     * @return true if already processed, false if new
     */
    private boolean checkIdempotency(String vnpayTransactionNo) {
        if (vnpayTransactionNo == null || vnpayTransactionNo.isEmpty()) {
            log.warn("Missing vnp_TransactionNo in IPN parameters");
            return false;
        }

        return idempotencyGuard.isAlreadyProcessed(vnpayTransactionNo);
    }

    /**
     * Update order status based on payment result
     * 
     * @param orderId Order ID to update
     * @param status Payment transaction status
     */
    private void updateOrderStatus(Long orderId, PaymentTransactionStatus status) {
        try {
            switch (status) {
                case COMPLETED:
                    // Payment successful - trigger order fulfillment
                    log.info("Payment completed, updating order status: orderId={}", orderId);
                    // Note: OrderService method needs to be implemented for payment completion
                    // orderService.markPaymentCompleted(orderId);
                    break;
                    
                case FAILED:
                    // Payment failed - mark order as payment failed
                    log.info("Payment failed, updating order status: orderId={}", orderId);
                    // Note: OrderService method needs to be implemented for payment failure
                    // orderService.markPaymentFailed(orderId);
                    break;
                    
                default:
                    log.warn("Unexpected payment status for order update: orderId={}, status={}", orderId, status);
                    break;
            }

        } catch (Exception e) {
            log.error("Failed to update order status: orderId={}, status={}, error={}", 
                     orderId, status, e.getMessage(), e);
            throw e; // Re-throw to trigger transaction rollback
        }
    }

    /**
     * Map VNPay response code to internal payment status
     * 
     * @param responseCode VNPay response code
     * @return PaymentTransactionStatus
     */
    private PaymentTransactionStatus mapResponseCodeToStatus(String responseCode) {
        if ("00".equals(responseCode)) {
            return PaymentTransactionStatus.COMPLETED;
        } else {
            return PaymentTransactionStatus.FAILED;
        }
    }
}