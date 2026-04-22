package com.codegym.smartphonemanagement.service.payment;

import com.codegym.smartphonemanagement.model.PaymentTransaction;
import com.codegym.smartphonemanagement.model.PaymentTransactionStatus;
import com.codegym.smartphonemanagement.model.TransactionLogStatus;
import com.codegym.smartphonemanagement.model.TransactionLogType;
import com.codegym.smartphonemanagement.repository.PaymentTransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Service for logging payment transactions to database
 * Provides comprehensive transaction tracking for audit, troubleshooting, and compliance
 * 
 * Features:
 * - Transaction lifecycle tracking (INITIATED → COMPLETED/FAILED)
 * - Atomic database operations with @Transactional
 * - Error logging with REQUIRES_NEW propagation for isolation
 * - Structured logging with transaction context
 * - Raw VNPay response storage for audit trail
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentTransactionLogger {

    private final PaymentTransactionRepository repository;
    private final ObjectMapper objectMapper;
    private final TransactionLogService auditLogService;

    /**
     * Log a new payment transaction with INITIATED status
     * Called when payment URL is created
     * 
     * @param orderId Order ID for the transaction
     * @param amount Payment amount in VND
     * @return Created PaymentTransaction entity
     */
    @Transactional
    public PaymentTransaction logInitiated(Long orderId, BigDecimal amount) {
        log.info("Logging INITIATED transaction for order ID: {}, amount: {}", orderId, amount);
        
        try {
            PaymentTransaction transaction = PaymentTransaction.builder()
                    .orderId(orderId)
                    .amount(amount)
                    .status(PaymentTransactionStatus.INITIATED)
                    .build();
            
            PaymentTransaction saved = repository.save(transaction);
            
            // Audit Log
            auditLogService.logTransaction(
                    String.valueOf(orderId),
                    TransactionLogType.VNPAY,
                    amount,
                    null, // User ID can be retrieved from Order if needed, but keeping it simple for now
                    TransactionLogStatus.PENDING,
                    "Khởi tạo giao dịch VNPay cho đơn hàng #" + orderId
            );

            log.info("Successfully logged INITIATED transaction ID: {} for order ID: {}", 
                    saved.getId(), orderId);
            
            return saved;
            
        } catch (Exception e) {
            log.error("Failed to log INITIATED transaction for order ID: {}", orderId, e);
            throw e;
        }
    }

    /**
     * Update payment transaction from VNPay callback
     * Updates status, VNPay transaction details, and stores raw response
     * 
     * @param vnpayTransactionNo VNPay transaction number from callback
     * @param vnpayResponse Full VNPay response parameters
     * @return Updated PaymentTransaction entity
     */
    @Transactional
    public PaymentTransaction updateFromCallback(String vnpayTransactionNo, 
                                                  Map<String, String> vnpayResponse) {
        log.info("Updating transaction from VNPay callback, transaction no: {}", vnpayTransactionNo);
        
        try {
            // Extract order ID from response
            String orderIdStr = vnpayResponse.get("vnp_TxnRef");
            if (orderIdStr == null) {
                log.error("Missing vnp_TxnRef in VNPay response");
                throw new IllegalArgumentException("Missing order ID in VNPay response");
            }
            
            Long orderId = Long.parseLong(orderIdStr);
            
            // Find existing transaction by order ID
            Optional<PaymentTransaction> existingOpt = repository.findByOrderId(orderId);
            
            PaymentTransaction transaction;
            if (existingOpt.isPresent()) {
                transaction = existingOpt.get();
                log.debug("Found existing transaction ID: {} for order ID: {}", 
                        transaction.getId(), orderId);
            } else {
                // Create new transaction if not found (fallback case)
                log.warn("No existing transaction found for order ID: {}, creating new one", orderId);
                transaction = PaymentTransaction.builder()
                        .orderId(orderId)
                        .amount(parseAmount(vnpayResponse.get("vnp_Amount")))
                        .status(PaymentTransactionStatus.INITIATED)
                        .build();
            }
            
            // Update transaction with VNPay response data
            String responseCode = vnpayResponse.get("vnp_ResponseCode");
            transaction.setVnpayTransactionNo(vnpayTransactionNo);
            transaction.setResponseCode(responseCode);
            transaction.setResponseMessage(vnpayResponse.get("vnp_OrderInfo"));
            transaction.setBankCode(vnpayResponse.get("vnp_BankCode"));
            transaction.setPaymentMethod(vnpayResponse.get("vnp_CardType"));
            
            // Determine status based on response code
            PaymentTransactionStatus status = "00".equals(responseCode) 
                    ? PaymentTransactionStatus.COMPLETED 
                    : PaymentTransactionStatus.FAILED;
            transaction.setStatus(status);
            
            // Store raw response as JSON for audit
            try {
                String rawResponse = objectMapper.writeValueAsString(vnpayResponse);
                transaction.setRawResponse(rawResponse);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize VNPay response to JSON", e);
                transaction.setRawResponse(vnpayResponse.toString());
            }
            
            PaymentTransaction saved = repository.save(transaction);
            
            // Audit Log
            auditLogService.logTransaction(
                    vnpayTransactionNo,
                    TransactionLogType.VNPAY,
                    transaction.getAmount(),
                    null, 
                    status == PaymentTransactionStatus.COMPLETED ? TransactionLogStatus.SUCCESS : TransactionLogStatus.FAILED,
                    "Cập nhật từ VNPay callback: " + transaction.getResponseMessage()
            );

            log.info("Successfully updated transaction ID: {} for order ID: {}, status: {}", 
                    saved.getId(), orderId, status);
            
            return saved;
            
        } catch (Exception e) {
            log.error("Failed to update transaction from VNPay callback, transaction no: {}", 
                    vnpayTransactionNo, e);
            throw e;
        }
    }

    /**
     * Log payment error with REQUIRES_NEW propagation
     * Ensures error is persisted even if parent transaction rolls back
     * 
     * @param orderId Order ID for the transaction
     * @param errorMessage Error message to log
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logError(Long orderId, String errorMessage) {
        log.error("Logging payment error for order ID: {}, error: {}", orderId, errorMessage);
        
        try {
            Optional<PaymentTransaction> existingOpt = repository.findByOrderId(orderId);
            
            PaymentTransaction transaction;
            if (existingOpt.isPresent()) {
                transaction = existingOpt.get();
            } else {
                // Create new transaction for error logging
                transaction = PaymentTransaction.builder()
                        .orderId(orderId)
                        .amount(BigDecimal.ZERO)
                        .status(PaymentTransactionStatus.FAILED)
                        .build();
            }
            
            transaction.setStatus(PaymentTransactionStatus.FAILED);
            transaction.setResponseMessage(errorMessage);
            
            repository.save(transaction);
            
            // Audit Log
            auditLogService.logTransaction(
                    "ERROR-" + orderId,
                    TransactionLogType.VNPAY,
                    BigDecimal.ZERO,
                    null,
                    TransactionLogStatus.FAILED,
                    "Lỗi thanh toán: " + errorMessage
            );

            log.info("Successfully logged error for order ID: {}", orderId);
            
        } catch (Exception e) {
            log.error("Failed to log error for order ID: {}", orderId, e);
            // Don't throw exception to avoid cascading failures
        }
    }

    /**
     * Find payment transaction by order ID
     * 
     * @param orderId Order ID to search for
     * @return Optional containing the transaction if found
     */
    @Transactional(readOnly = true)
    public Optional<PaymentTransaction> findByOrderId(Long orderId) {
        log.debug("Finding transaction by order ID: {}", orderId);
        return repository.findByOrderId(orderId);
    }

    /**
     * Find payment transaction by VNPay transaction number
     * 
     * @param vnpayTransactionNo VNPay transaction number to search for
     * @return Optional containing the transaction if found
     */
    @Transactional(readOnly = true)
    public Optional<PaymentTransaction> findByVnpayTransactionNo(String vnpayTransactionNo) {
        log.debug("Finding transaction by VNPay transaction no: {}", vnpayTransactionNo);
        return repository.findByVnpayTransactionNo(vnpayTransactionNo);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Parse amount from VNPay format (amount * 100) back to VND
     */
    private BigDecimal parseAmount(String amountStr) {
        try {
            if (amountStr == null || amountStr.trim().isEmpty()) {
                return BigDecimal.ZERO;
            }
            long amountInCents = Long.parseLong(amountStr);
            return new BigDecimal(amountInCents).divide(new BigDecimal(100));
        } catch (NumberFormatException e) {
            log.warn("Failed to parse amount: {}", amountStr);
            return BigDecimal.ZERO;
        }
    }
}
