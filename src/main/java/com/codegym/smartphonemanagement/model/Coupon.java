package com.codegym.smartphonemanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version")
    private Long version; // For optimistic locking to prevent race conditions

    @NotBlank(message = "Mã giảm giá không được rỗng")
    @Column(unique = true, nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CouponCategory couponCategory = CouponCategory.PRODUCT_DISCOUNT;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.0")
    private BigDecimal maxDiscountAmount;

    @DecimalMin(value = "0.0")
    private BigDecimal minOrderValue;

    @Min(0)
    private Integer maxUsageGlobal;

    @Min(0)
    @Column(nullable = false)
    @Builder.Default
    private Integer currentUsageGlobal = 0;

    @Min(1)
    private Integer perUserLimit;

    @NotNull
    private LocalDateTime startDate;

    @NotNull
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CouponStatus status = CouponStatus.ACTIVE;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "coupon_applicable_products",
            joinColumns = @JoinColumn(name = "coupon_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private List<Product> applicableProducts;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "coupon_applicable_categories",
            joinColumns = @JoinColumn(name = "coupon_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private List<Category> applicableCategories;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "coupon_applicable_brands", joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "brand")
    private List<String> applicableBrands;

    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "coupon_applicable_payment_methods", joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "payment_method")
    private List<PaymentMethod> applicablePaymentMethods;
    
    @Column(length = 200)
    private String description;

    @PrePersist
    public void prePersist() {
        if (this.currentUsageGlobal == null) {
            this.currentUsageGlobal = 0;
        }
        if (this.status == null) {
            this.status = CouponStatus.ACTIVE;
        }
    }
}
