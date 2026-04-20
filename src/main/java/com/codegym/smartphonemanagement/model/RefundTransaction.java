package com.codegym.smartphonemanagement.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a refund transaction record.
 * Stores comprehensive refund transaction data for audit, troubleshooting, and compliance purposes.
 */
@Entity
@Table(name = "refund_transactions", 
    indexes = {
        @Index(name = "idx_refund_order_id", columnList = "order_id"),
        @Index(name = "idx_refund_request_id", columnList = "refund_request_id"),
        @Index(name = "idx_refund_status", columnList = "status"),
        @Index(name = "idx_refund_created_at", columnList = "created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_refund_request_id", columnNames = "refund_request_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "refund_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "original_transaction_no", nullable = false)
    private String originalTransactionNo;

    @Column(name = "refund_request_id", nullable = false, unique = true)
    private String refundRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RefundStatus status;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "vnpay_refund_transaction_no")
    private String vnpayRefundTransactionNo;

    @Column(name = "response_code")
    private String responseCode;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    /**
     * Automatically set timestamps before persisting
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Set completion timestamp when refund is completed
     */
    public void markAsCompleted() {
        this.completedAt = LocalDateTime.now();
        this.status = RefundStatus.COMPLETED;
    }

    /**
     * Set status to failed
     */
    public void markAsFailed() {
        this.status = RefundStatus.FAILED;
    }
}
