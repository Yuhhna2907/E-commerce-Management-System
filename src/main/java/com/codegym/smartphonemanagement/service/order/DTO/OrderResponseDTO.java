package com.codegym.smartphonemanagement.service.order.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class OrderResponseDTO {
    private Long id;
    private BigDecimal totalPrice;
    private BigDecimal totalDiscount;
    private BigDecimal shippingFee;
    private String couponCode;
    private String status;
    private LocalDateTime createdAt;
    private String customerName;
    private String receiverPhone;
    private String shippingAddress;
    private String note;
    private List<OrderItemResponseDTO> items;
    private String paymentMethodDisplay;
    private String paymentStatus;
    private String paymentStatusDisplay;
    // OMS timeline
    private List<OrderHistoryDTO> timeline;
    // Refund info (nếu có)
    private RefundResponseDTO activeRefund;
    // Trạng thái hiển thị tiếng Việt
    private String statusDisplay;
    // Cho user biết có thể hủy / refund không
    private boolean canCancel;
    private boolean canRefund;
    // Ngày giao hàng dự kiến (3-5 ngày làm việc)
    private LocalDateTime estimatedDeliveryDate;
}