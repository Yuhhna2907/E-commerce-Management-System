package com.codegym.smartphonemanagement.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lịch sử giao dịch ví SmartZone Xu của user.
 */
@Entity
@Table(name = "sz_wallet_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SzWalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private SzWallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WalletTransactionType type;

    /** Số tiền giao dịch (luôn dương — dấu xác định bởi type) */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** Số dư sau giao dịch (snapshot để debug) */
    @Column(precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(length = 300)
    private String description;

    /** Admin ghi chú thêm (khi admin thao tác) */
    @Column(length = 300)
    private String adminNote;

    /** Đơn hàng liên quan (nullable) */
    @Column(name = "related_order_id")
    private Long relatedOrderId;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() { this.createdAt = LocalDateTime.now(); }
}
