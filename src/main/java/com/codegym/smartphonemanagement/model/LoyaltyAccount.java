package com.codegym.smartphonemanagement.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    /**
     * Điểm hiện có (có thể bị trừ khi đổi điểm hoặc refund)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer totalPoints = 0;

    /**
     * Tổng điểm tích lũy vĩnh viễn (chỉ tăng, không giảm) — dùng để phân tier sau này
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer lifetimePoints = 0;

    // FIX #13: Thêm version cho optimistic locking
    @Version
    private Long version;

    private LocalDateTime updatedAt;

    @PreUpdate
    @PrePersist
    public void prePersist() {
        this.updatedAt = LocalDateTime.now();
    }
}
