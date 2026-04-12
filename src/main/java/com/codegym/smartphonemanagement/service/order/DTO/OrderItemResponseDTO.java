package com.codegym.smartphonemanagement.service.order.DTO;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class OrderItemResponseDTO {
    private Long id;  // OrderItem ID (cần cho Refund form)
    private Long productId;
    private Long variantId;
    private String productName;
    private String variantName;
    private String imageUrl;
    private Integer quantity;
    private BigDecimal price; // Giá tại thời điểm chốt đơn
    private BigDecimal subTotal;
    private BigDecimal allocatedDiscount; // Giảm giá Voucher phân bổ
    private BigDecimal netPrice; // Giá thực sau giảm = subTotal - allocatedDiscount
}