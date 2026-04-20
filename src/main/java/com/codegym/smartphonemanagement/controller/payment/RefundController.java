package com.codegym.smartphonemanagement.controller.payment;

import com.codegym.smartphonemanagement.exception.PaymentException;
import com.codegym.smartphonemanagement.model.RefundTransaction;
import com.codegym.smartphonemanagement.service.payment.RefundService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for refund operations
 * Provides endpoints for creating refunds and checking refund status
 */
@RestController
@RequestMapping("/api/payment/refund")
@RequiredArgsConstructor
@Slf4j
public class RefundController {

    private final RefundService refundService;

    /**
     * Create a refund request
     * POST /api/payment/refund
     *
     * @param request Refund request containing orderId, refundAmount, and refundReason
     * @return RefundTransaction response with refund details
     */
    @PostMapping
    public ResponseEntity<?> createRefund(
            @Valid @RequestBody RefundRequest request,
            BindingResult bindingResult
    ) {
        log.info("Received refund request: orderId={}, amount={}", request.getOrderId(), request.getRefundAmount());

        try {
            // Validate request
            if (bindingResult.hasErrors()) {
                Map<String, String> errors = bindingResult.getFieldErrors().stream()
                        .collect(Collectors.toMap(
                                error -> error.getField(),
                                error -> error.getDefaultMessage()
                        ));
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Validation failed",
                        "errors", errors
                ));
            }

            // Create refund
            RefundTransaction refundTransaction = refundService.createRefund(
                    request.getOrderId(),
                    request.getRefundAmount(),
                    request.getRefundReason()
            );

            // Build response
            RefundResponse response = RefundResponse.builder()
                    .success(true)
                    .message("Refund request created successfully")
                    .refundRequestId(refundTransaction.getRefundRequestId())
                    .orderId(refundTransaction.getOrderId())
                    .refundAmount(refundTransaction.getRefundAmount())
                    .status(refundTransaction.getStatus().name())
                    .reason(refundTransaction.getReason())
                    .createdAt(refundTransaction.getCreatedAt().toString())
                    .build();

            log.info("Refund request created successfully: requestId={}, status={}", 
                    refundTransaction.getRefundRequestId(), refundTransaction.getStatus());

            return ResponseEntity.ok(response);

        } catch (PaymentException e) {
            log.error("Refund request failed: orderId={}, error={}", request.getOrderId(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Unexpected error during refund request: orderId={}, error={}", 
                    request.getOrderId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Internal server error: " + e.getMessage()
            ));
        }
    }

    /**
     * Check refund status
     * GET /api/payment/refund/{refundRequestId}/status
     *
     * @param refundRequestId Refund request ID to check
     * @return RefundTransaction response with current status
     */
    @GetMapping("/{refundRequestId}/status")
    public ResponseEntity<?> checkRefundStatus(@PathVariable String refundRequestId) {
        log.info("Checking refund status: requestId={}", refundRequestId);

        try {
            // Check status
            RefundTransaction refundTransaction = refundService.checkRefundStatus(refundRequestId);

            // Build response
            RefundStatusResponse response = RefundStatusResponse.builder()
                    .success(true)
                    .message("Refund status retrieved successfully")
                    .refundRequestId(refundTransaction.getRefundRequestId())
                    .orderId(refundTransaction.getOrderId())
                    .refundAmount(refundTransaction.getRefundAmount())
                    .status(refundTransaction.getStatus().name())
                    .reason(refundTransaction.getReason())
                    .createdAt(refundTransaction.getCreatedAt().toString())
                    .completedAt(refundTransaction.getCompletedAt() != null ? 
                            refundTransaction.getCompletedAt().toString() : null)
                    .vnpayRefundTransactionNo(refundTransaction.getVnpayRefundTransactionNo())
                    .responseCode(refundTransaction.getResponseCode())
                    .build();

            log.info("Refund status retrieved: requestId={}, status={}", 
                    refundRequestId, refundTransaction.getStatus());

            return ResponseEntity.ok(response);

        } catch (PaymentException e) {
            log.error("Failed to check refund status: requestId={}, error={}", refundRequestId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Unexpected error checking refund status: requestId={}, error={}", 
                    refundRequestId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Internal server error: " + e.getMessage()
            ));
        }
    }

    /**
     * Request DTO for creating refund
     */
    @Data
    public static class RefundRequest {
        @NotNull(message = "Order ID is required")
        private Long orderId;

        @NotNull(message = "Refund amount is required")
        @DecimalMin(value = "0.01", message = "Refund amount must be greater than 0")
        private BigDecimal refundAmount;

        @NotBlank(message = "Refund reason is required")
        private String refundReason;
    }

    /**
     * Response DTO for refund creation
     */
    @Data
    @lombok.Builder
    public static class RefundResponse {
        private boolean success;
        private String message;
        private String refundRequestId;
        private Long orderId;
        private BigDecimal refundAmount;
        private String status;
        private String reason;
        private String createdAt;
    }

    /**
     * Response DTO for refund status check
     */
    @Data
    @lombok.Builder
    public static class RefundStatusResponse {
        private boolean success;
        private String message;
        private String refundRequestId;
        private Long orderId;
        private BigDecimal refundAmount;
        private String status;
        private String reason;
        private String createdAt;
        private String completedAt;
        private String vnpayRefundTransactionNo;
        private String responseCode;
    }
}
