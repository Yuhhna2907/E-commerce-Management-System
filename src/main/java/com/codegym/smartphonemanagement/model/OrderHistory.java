package com.codegym.smartphonemanagement.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_from")
    private OrderStatus statusFrom;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_to", nullable = false)
    private OrderStatus statusTo;

    @Column(name = "updated_by", length = 100)
    private String updatedBy; // "SYSTEM", "ADMIN", "USER:username"

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
