package com.codegym.smartphonemanagement.service.payment;

import com.codegym.smartphonemanagement.config.payment.VNPAYConfig;
import com.codegym.smartphonemanagement.exception.PaymentException;
import com.codegym.smartphonemanagement.model.*;
import com.codegym.smartphonemanagement.model.dto.payment.VNPayRefundRequest;
import com.codegym.smartphonemanagement.model.dto.payment.VNPayRefundResponse;
import com.codegym.smartphonemanagement.model.dto.payment.VNPayRefundStatusResponse;
import com.codegym.smartphonemanagement.repository.PaymentTransactionRepository;
import com.codegym.smartphonemanagement.repository.RefundTransactionRepository;
import com.codegym.smartphonemanagement.repository.user.OrderRepository;
import com.codegym.smartphonemanagement.util.payment.RefundResponseParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for managing refund operations
 * Handles refund request creation, validation, and status tracking
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final VNPayAPIClient apiClient;
    private final RefundTransactionRepository refundRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final VNPAYConfig vnpayConfig;
    private final RefundResponseParser refundResponseParser;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Create a refund request for an order
     * Validates order eligibility and submits refund to VNPay API
     *
     * @param orderId Order ID to refund
     * @param refundAmount Amount to refund
     * @param refundReason Reason for refund
     * @return RefundTransaction record
     * @throws PaymentException if validation fails or API call fails
     */
    @Transactional
    public RefundTransaction createRefund(Long orderId, BigDecimal refundAmount, String refundReason) {
        log.info("Creating refund request: orderId={}, amount={}, reason={}", orderId, refundAmount, refundReason);

        try {
            // Validate refund request
            Order order = validateRefundRequest(orderId, refundAmount);

            // Get original payment transaction
            PaymentTransaction paymentTransaction = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new PaymentException("Payment transaction not found for order: " + orderId));

            // Generate unique refund request ID
            String refundRequestId = generateRefundRequestId(orderId);

            // Build refund request
            VNPayRefundRequest refundRequest = buildRefundRequest(
                    paymentTransaction,
                    refundAmount,
                    refundRequestId,
                    refundReason
            );

            // Create refund transaction record with PENDING status
            RefundTransaction refundTransaction = RefundTransaction.builder()
                    .orderId(orderId)
                    .refundAmount(refundAmount)
                    .originalTransactionNo(paymentTransaction.getVnpayTransactionNo())
                    .refundRequestId(refundRequestId)
                    .status(RefundStatus.PENDING)
                    .reason(refundReason)
                    .build();

            refundTransaction = refundRepository.save(refundTransaction);
            log.info("Created refund transaction record: id={}, requestId={}", 
                    refundTransaction.getId(), refundRequestId);

            // Submit refund to VNPay API
            VNPayRefundResponse response = apiClient.submitRefund(refundRequest);

            // Update refund transaction with response
            updateRefundTransactionFromResponse(refundTransaction, response);

            // Update order payment status if refund completed
            if (refundTransaction.getStatus() == RefundStatus.COMPLETED) {
                updateOrderPaymentStatus(order, refundAmount);
            }

            log.info("Refund request completed: requestId={}, status={}, responseCode={}", 
                    refundRequestId, refundTransaction.getStatus(), response.getVnp_ResponseCode());

            return refundTransaction;

        } catch (PaymentException e) {
            log.error("Refund request failed: orderId={}, error={}", orderId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during refund request: orderId={}, error={}", orderId, e.getMessage(), e);
            throw new PaymentException("Refund request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Check refund status from VNPay API
     * Updates refund transaction record with current status
     *
     * @param refundRequestId Refund request ID to check
     * @return Updated RefundTransaction record
     * @throws PaymentException if refund not found or API call fails
     */
    @Transactional
    public RefundTransaction checkRefundStatus(String refundRequestId) {
        log.info("Checking refund status: requestId={}", refundRequestId);

        try {
            // Get refund transaction record
            RefundTransaction refundTransaction = refundRepository.findByRefundRequestId(refundRequestId)
                    .orElseThrow(() -> new PaymentException("Refund transaction not found: " + refundRequestId));

            // Check status from VNPay API
            VNPayRefundStatusResponse response = apiClient.checkRefundStatus(refundRequestId);

            // Update refund transaction with response
            RefundStatus newStatus = refundResponseParser.mapResponseCodeToStatus(response.getVnp_ResponseCode());
            refundTransaction.setStatus(newStatus);
            refundTransaction.setResponseCode(response.getVnp_ResponseCode());

            if (response.getVnp_TransactionNo() != null) {
                refundTransaction.setVnpayRefundTransactionNo(response.getVnp_TransactionNo());
            }

            if (newStatus == RefundStatus.COMPLETED) {
                refundTransaction.markAsCompleted();

                // Update order payment status
                RefundTransaction finalRefundTransaction = refundTransaction;
                Order order = orderRepository.findById(refundTransaction.getOrderId())
                        .orElseThrow(() -> new PaymentException("Order not found: " + finalRefundTransaction.getOrderId()));
                updateOrderPaymentStatus(order, refundTransaction.getRefundAmount());
            }

            refundTransaction = refundRepository.save(refundTransaction);

            log.info("Refund status updated: requestId={}, status={}, responseCode={}", 
                    refundRequestId, newStatus, response.getVnp_ResponseCode());

            return refundTransaction;

        } catch (PaymentException e) {
            log.error("Failed to check refund status: requestId={}, error={}", refundRequestId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error checking refund status: requestId={}, error={}", 
                    refundRequestId, e.getMessage(), e);
            throw new PaymentException("Failed to check refund status: " + e.getMessage(), e);
        }
    }

    /**
     * Validate refund request eligibility
     * Checks order exists, payment completed, and refund amount valid
     */
    private Order validateRefundRequest(Long orderId, BigDecimal refundAmount) {
        // Validate order exists
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new PaymentException("Order not found: " + orderId));

        // Validate payment status is COMPLETED
        if (order.getPaymentStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentException("Cannot refund order with payment status: " + order.getPaymentStatus());
        }

        // Validate refund amount does not exceed original amount
        if (refundAmount.compareTo(order.getTotalPrice()) > 0) {
            throw new PaymentException(String.format(
                    "Refund amount (%s) exceeds original payment amount (%s)",
                    refundAmount, order.getTotalPrice()
            ));
        }

        // Validate refund amount is positive
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("Refund amount must be greater than zero");
        }

        log.debug("Refund request validation passed: orderId={}, amount={}", orderId, refundAmount);
        return order;
    }

    /**
     * Generate unique refund request ID
     * Format: ORD{orderId}_REF{timestamp}
     */
    private String generateRefundRequestId(Long orderId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        return String.format("ORD%d_REF%s", orderId, timestamp);
    }

    /**
     * Build VNPay refund request from payment transaction
     */
    private VNPayRefundRequest buildRefundRequest(
            PaymentTransaction paymentTransaction,
            BigDecimal refundAmount,
            String refundRequestId,
            String refundReason
    ) {
        // Determine transaction type (02 = full refund, 03 = partial refund)
        String transactionType = refundAmount.compareTo(paymentTransaction.getAmount()) == 0 ? "02" : "03";

        // Format dates
        String createDate = LocalDateTime.now().format(DATE_FORMATTER);
        String transactionDate = paymentTransaction.getCreatedAt().format(DATE_FORMATTER);

        // Build request
        VNPayRefundRequest request = VNPayRefundRequest.builder()
                .vnp_RequestId(refundRequestId)
                .vnp_Version("2.1.0")
                .vnp_Command("refund")
                .vnp_TmnCode(vnpayConfig.getVnp_TmnCode())
                .vnp_TransactionType(transactionType)
                .vnp_TxnRef(String.valueOf(paymentTransaction.getOrderId()))
                .vnp_Amount(refundAmount.multiply(BigDecimal.valueOf(100))) // Convert to VNPay format (x100)
                .vnp_OrderInfo(refundReason)
                .vnp_TransactionNo(paymentTransaction.getVnpayTransactionNo())
                .vnp_TransactionDate(transactionDate)
                .vnp_CreateBy("SYSTEM") // TODO: Get from authenticated user
                .vnp_CreateDate(createDate)
                .vnp_IpAddr("127.0.0.1") // TODO: Get from request context
                .build();

        // Generate signature
        String hashData = buildRefundHashData(request);
        String secureHash = VNPAYConfig.hmacSHA512(vnpayConfig.getVnp_HashSecret(), hashData);
        request.setVnp_SecureHash(secureHash);

        return request;
    }

    /**
     * Build hash data for refund request signature
     */
    private String buildRefundHashData(VNPayRefundRequest request) {
        return String.format(
                "vnp_Amount=%s&vnp_Command=%s&vnp_CreateBy=%s&vnp_CreateDate=%s&vnp_IpAddr=%s&" +
                "vnp_OrderInfo=%s&vnp_RequestId=%s&vnp_TmnCode=%s&vnp_TransactionDate=%s&" +
                "vnp_TransactionNo=%s&vnp_TransactionType=%s&vnp_TxnRef=%s&vnp_Version=%s",
                request.getVnp_Amount(),
                request.getVnp_Command(),
                request.getVnp_CreateBy(),
                request.getVnp_CreateDate(),
                request.getVnp_IpAddr(),
                request.getVnp_OrderInfo(),
                request.getVnp_RequestId(),
                request.getVnp_TmnCode(),
                request.getVnp_TransactionDate(),
                request.getVnp_TransactionNo(),
                request.getVnp_TransactionType(),
                request.getVnp_TxnRef(),
                request.getVnp_Version()
        );
    }

    /**
     * Update refund transaction from VNPay response
     */
    private void updateRefundTransactionFromResponse(
            RefundTransaction refundTransaction,
            VNPayRefundResponse response
    ) {
        RefundStatus status = refundResponseParser.mapResponseCodeToStatus(response.getVnp_ResponseCode());
        refundTransaction.setStatus(status);
        refundTransaction.setResponseCode(response.getVnp_ResponseCode());

        if (response.getVnp_TransactionNo() != null) {
            refundTransaction.setVnpayRefundTransactionNo(response.getVnp_TransactionNo());
        }

        // Store raw response for audit
        refundTransaction.setRawResponse(String.format(
                "responseCode=%s,message=%s,transactionNo=%s,amount=%s,bankCode=%s,payDate=%s",
                response.getVnp_ResponseCode(),
                response.getVnp_Message(),
                response.getVnp_TransactionNo(),
                response.getVnp_Amount(),
                response.getVnp_BankCode(),
                response.getVnp_PayDate()
        ));

        if (status == RefundStatus.COMPLETED) {
            refundTransaction.markAsCompleted();
        } else if (status == RefundStatus.FAILED) {
            refundTransaction.markAsFailed();
        }

        refundRepository.save(refundTransaction);
    }

    /**
     * Update order payment status based on refund amount
     * Full refund -> REFUNDED
     * Partial refund -> remains COMPLETED
     */
    private void updateOrderPaymentStatus(Order order, BigDecimal refundAmount) {
        boolean isFullRefund = refundAmount.compareTo(order.getTotalPrice()) == 0;

        if (isFullRefund) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            orderRepository.save(order);
            log.info("Order payment status updated to REFUNDED: orderId={}", order.getId());
        } else {
            log.info("Partial refund - order payment status remains COMPLETED: orderId={}, refundAmount={}", 
                    order.getId(), refundAmount);
        }
    }
}
