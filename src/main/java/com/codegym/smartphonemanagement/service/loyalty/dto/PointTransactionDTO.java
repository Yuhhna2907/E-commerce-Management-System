package com.codegym.smartphonemanagement.service.loyalty.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointTransactionDTO {
    private Long id;
    private String type;          // Enum name: EARNED, REDEEMED, REFUND_DEDUCTED, ADMIN_ADJUST
    private String typeDisplay;   // "Tích điểm", "Đổi điểm", "Trừ điểm hoàn trả", "Điều chỉnh"
    private String typeIcon;      // Bootstrap icon class
    private String typeCssClass;  // CSS class cho màu sắc
    private Integer points;       // Dương = cộng, âm = trừ
    private String description;
    private Long orderId;
    private LocalDateTime createdAt;
}
