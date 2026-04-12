package com.codegym.smartphonemanagement.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "refund_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_request_id", nullable = false)
    private RefundRequest refundRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(nullable = false)
    private Integer quantity; // Số lượng trả lại

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice; // Giá đơn vị tại thời điểm mua

    @Column(name = "allocated_discount", nullable = false)
    private BigDecimal allocatedDiscount; // Giảm giá phân bổ cho phần trả

    @Column(name = "refund_amount", nullable = false)
    private BigDecimal refundAmount; // Số tiền thực hoàn = (unitPrice * qty) - allocatedDiscount
}
