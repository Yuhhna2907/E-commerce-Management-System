package com.codegym.smartphonemanagement.service.order.DTO;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefundItemDetailDTO {
    private Long orderItemId;
    private String productName;
    private String variantName;
    private String imageUrl;
    private Integer quantityOrdered;    // Đã mua
    private Integer quantityReturned;   // Trả lại
    private BigDecimal unitPrice;
    private BigDecimal allocatedDiscountPerUnit; // Khấu hao Voucher / đơn vị
    private BigDecimal refundAmount;    // Số tiền thực hoàn cho dòng này
}
