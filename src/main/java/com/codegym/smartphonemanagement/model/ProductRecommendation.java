package com.codegym.smartphonemanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity cho bảng product_recommendations
 * Lưu trữ thông tin gợi ý sản phẩm dựa trên lịch sử mua chung
 * 
 * Yêu cầu: 5.2, 6.1
 */
@Entity
@Table(name = "product_recommendations",
       uniqueConstraints = {
           @UniqueConstraint(
               name = "uk_product_recommendation",
               columnNames = {"product_id", "recommended_product_id"}
           )
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRecommendation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Sản phẩm gốc (sản phẩm mà người dùng đang xem)
     */
    @NotNull(message = "Product không được null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    /**
     * Sản phẩm được gợi ý (sản phẩm thường được mua cùng)
     */
    @NotNull(message = "Recommended product không được null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_product_id", nullable = false)
    private Product recommendedProduct;
    
    /**
     * Số lần hai sản phẩm được mua chung trong cùng một đơn hàng
     */
    @NotNull(message = "Co-purchase count không được null")
    @Min(value = 0, message = "Co-purchase count phải >= 0")
    @Column(name = "co_purchase_count", nullable = false)
    private Integer coPurchaseCount = 0;
    
    /**
     * Tần suất mua chung (0.0000 - 1.0000)
     * Công thức: số đơn hàng chứa cả 2 sản phẩm / tổng số đơn hàng chứa sản phẩm gốc
     */
    @NotNull(message = "Co-purchase frequency không được null")
    @DecimalMin(value = "0.0000", message = "Co-purchase frequency phải >= 0.0000")
    @DecimalMax(value = "1.0000", message = "Co-purchase frequency phải <= 1.0000")
    @Column(name = "co_purchase_frequency", nullable = false, precision = 5, scale = 4)
    private BigDecimal coPurchaseFrequency = BigDecimal.ZERO;
    
    /**
     * Thời điểm cập nhật cuối cùng
     */
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
    
    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.lastUpdated = LocalDateTime.now();
    }
    
    /**
     * Kiểm tra xem có phải là gợi ý hợp lệ không
     * (tần suất >= 1% và số lần mua chung >= 2) - ADJUSTED FOR DEVELOPMENT
     */
    public boolean isValidRecommendation() {
        return coPurchaseFrequency.compareTo(new BigDecimal("0.0100")) >= 0 
               && coPurchaseCount >= 2;
    }
    
    /**
     * Lấy tần suất mua chung dưới dạng phần trăm
     */
    public double getFrequencyAsPercentage() {
        return coPurchaseFrequency.multiply(new BigDecimal("100")).doubleValue();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductRecommendation)) return false;
        ProductRecommendation that = (ProductRecommendation) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "ProductRecommendation{" +
                "id=" + id +
                ", productId=" + (product != null ? product.getId() : null) +
                ", recommendedProductId=" + (recommendedProduct != null ? recommendedProduct.getId() : null) +
                ", coPurchaseCount=" + coPurchaseCount +
                ", coPurchaseFrequency=" + coPurchaseFrequency +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}