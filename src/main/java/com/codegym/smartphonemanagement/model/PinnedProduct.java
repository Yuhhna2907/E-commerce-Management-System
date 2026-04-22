package com.codegym.smartphonemanagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for manually pinned products on homepage
 * Allows admin to feature specific products above AI recommendations
 */
@Entity
@Table(name = "pinned_product")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinnedProduct {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
    
    @Column(name = "active", nullable = false)
    private Boolean active = true;
    
    @Column(name = "pinned_at", nullable = false)
    private LocalDateTime pinnedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pinned_by_user_id")
    private User pinnedByUser;
    
    @PrePersist
    protected void onCreate() {
        if (pinnedAt == null) {
            pinnedAt = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
    }
}
