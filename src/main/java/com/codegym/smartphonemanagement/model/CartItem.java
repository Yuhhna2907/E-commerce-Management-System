package com.codegym.smartphonemanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Min(value = 1, message = "Số lượng phải >= 1")
    private Integer quantity;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @NotNull(message = "Product không được null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Giá sản phẩm tại thời điểm thêm vào giỏ
    @NotNull
    @Column(name = "price_at_time", precision = 15, scale = 2)
    private BigDecimal priceAtTime;

    // Tổng tiền = quantity * priceAtTime
    @NotNull
    @Column(name = "total_price", precision = 15, scale = 2)
    private BigDecimal totalPrice;
}