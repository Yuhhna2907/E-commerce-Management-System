package com.codegym.smartphonemanagement.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ví SmartZone Xu — lưu số dư tiền thật (VND) của mỗi User.
 * Dùng để nhận tiền hoàn trả, bồi thường, và thanh toán đơn hàng.
 */
@Entity
@Table(name = "sz_wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SzWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    /** Số dư hiện tại (VND). Không cho phép âm. */
    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    /** Tổng tiền đã nhận vào (mọi thời điểm, chỉ tăng) */
    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalIn = BigDecimal.ZERO;

    /** Tổng tiền đã tiêu / rút ra (mọi thời điểm, chỉ tăng) */
    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalOut = BigDecimal.ZERO;

    @Version
    private Long version;

    private LocalDateTime updatedAt;

    @PreUpdate
    @PrePersist
    public void prePersist() {
        this.updatedAt = LocalDateTime.now();
    }
}
