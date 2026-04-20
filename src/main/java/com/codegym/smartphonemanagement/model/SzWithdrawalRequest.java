package com.codegym.smartphonemanagement.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Yêu cầu rút tiền SmartZone Xu ra Ngân Hàng của User.
 * Admin xem và duyệt/từ chối thủ công.
 */
@Entity
@Table(name = "sz_withdrawal_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SzWithdrawalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 100)
    private String bankName;

    @Column(nullable = false, length = 50)
    private String bankAccount;

    @Column(nullable = false, length = 100)
    private String bankHolder;

    /** Ghi chú từ user (tùy chọn) */
    @Column(length = 300)
    private String userNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WithdrawalStatus status = WithdrawalStatus.PENDING;

    /** Admin ghi chú khi duyệt / từ chối */
    @Column(length = 300)
    private String adminNote;

    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    @PrePersist
    public void prePersist() { this.createdAt = LocalDateTime.now(); }
}
