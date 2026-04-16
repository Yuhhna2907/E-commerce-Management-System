package com.codegym.smartphonemanagement.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_for_later")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedForLater {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "variant_id")
    private Long variantId;
    
    @Column(nullable = false)
    private Integer quantity = 1;
    
    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;
    
    @Column(length = 500)
    private String note;
    
    @PrePersist
    protected void onCreate() {
        savedAt = LocalDateTime.now();
    }
}
