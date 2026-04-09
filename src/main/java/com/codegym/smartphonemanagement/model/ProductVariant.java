package com.codegym.smartphonemanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long variantId;

    @NotBlank(message = "SKU không được trống")
    @Column(unique = true, nullable = false)
    private String sku;   // Mã định danh riêng cho biến thể

    @NotBlank(message = "Tên biến thể không được trống")
    private String variantName; // Ví dụ: iPhone 17 - Xanh - 512GB

    private String color;
    private String storage;
    private String ram;

    @NotNull(message = "Giá nhập không được null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá nhập phải > 0")
    private BigDecimal costPrice;

    @NotNull(message = "Giá bán không được null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá bán phải > 0")
    private BigDecimal salePrice;

    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private Integer stockQuantity;

    @Column(nullable = false)
    private Boolean isActive = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Liên kết với Product
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Liên kết với CartItem
    @OneToMany(mappedBy = "productVariant", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<CartItem> cartItems;

    // Liên kết với OrderItem
    @OneToMany(mappedBy = "productVariant", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<OrderItem> orderItems;


    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

