package com.codegym.smartphonemanagement.service.payment;

import com.codegym.smartphonemanagement.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for preventing duplicate webhook processing using VNPay transaction numbers
 * Provides idempotency guarantees for webhook notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyGuard {

    private final PaymentTransactionRepository repository;

    /**
     * Check if a VNPay transaction has already been processed
     * Uses VNPay transaction number as natural idempotency key
     * 
     * @param vnpayTransactionNo VNPay transaction number from webhook
     * @return true if transaction has already been processed, false otherwise
     */
    public boolean isAlreadyProcessed(String vnpayTransactionNo) {
        if (vnpayTransactionNo == null || vnpayTransactionNo.trim().isEmpty()) {
            log.warn("Idempotency check failed: null or empty VNPay transaction number");
            return false;
        }

        try {
            boolean exists = repository.findByVnpayTransactionNo(vnpayTransactionNo).isPresent();
            
            if (exists) {
                log.info("Duplicate webhook detected: vnpayTransactionNo={}", vnpayTransactionNo);
            } else {
                log.debug("New webhook detected: vnpayTransactionNo={}", vnpayTransactionNo);
            }
            
            return exists;
            
        } catch (Exception e) {
            log.error("Error checking idempotency for vnpayTransactionNo={}: {}", 
                     vnpayTransactionNo, e.getMessage(), e);
            // In case of error, assume not processed to avoid blocking legitimate webhooks
            return false;
        }
    }

    /**
     * Mark a VNPay transaction as processed
     * This method is typically called by PaymentTransactionLogger when creating/updating transactions
     * 
     * Note: This method doesn't create the transaction record itself, it's just a marker
     * The actual transaction creation is handled by PaymentTransactionLogger
     * 
     * @param vnpayTransactionNo VNPay transaction number to mark as processed
     */
    @Transactional
    public void markAsProcessed(String vnpayTransactionNo) {
        if (vnpayTransactionNo == null || vnpayTransactionNo.trim().isEmpty()) {
            log.warn("Cannot mark as processed: null or empty VNPay transaction number");
            return;
        }

        try {
            // Check if already exists
            if (repository.findByVnpayTransactionNo(vnpayTransactionNo).isPresent()) {
                log.debug("Transaction already marked as processed: vnpayTransactionNo={}", vnpayTransactionNo);
                return;
            }

            log.info("Marked transaction as processed: vnpayTransactionNo={}", vnpayTransactionNo);
            
            // Note: The actual transaction record creation is handled by PaymentTransactionLogger
            // This method serves as a conceptual marker for idempotency
            
        } catch (Exception e) {
            log.error("Error marking transaction as processed for vnpayTransactionNo={}: {}", 
                     vnpayTransactionNo, e.getMessage(), e);
            throw e; // Re-throw to trigger transaction rollback
        }
    }
}