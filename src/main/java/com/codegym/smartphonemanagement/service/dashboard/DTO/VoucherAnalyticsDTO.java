package com.codegym.smartphonemanagement.service.dashboard.DTO;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherAnalyticsDTO {

    // ===== NHÓM 1: HIỆU NĂNG =====
    private Long totalActiveVouchers;
    private Long totalExpiredVouchers;
    private Long totalPausedVouchers;
    private Long totalCancelledVouchers;

    private Long totalUsageCount;           // Tổng lượt dùng
    private BigDecimal usageRate;           // % Tỷ lệ sử dụng (used / max_usage_global)
    private BigDecimal voucherSaturation;   // % đơn hàng có dùng voucher
    private BigDecimal totalDiscountBurned; // Tổng tiền đã giảm
    private BigDecimal totalRevenueGenerated; // Doanh thu từ đơn có voucher
    private BigDecimal overallROI;          // (revenue - discount) / discount * 100
    private BigDecimal aovWithVoucher;      // AOV khi có voucher (trong kỳ hiệu lực)
    private BigDecimal aovWithoutVoucher;   // AOV khi không có voucher
    private BigDecimal aovLift;             // % tăng trưởng AOV nhờ voucher

    // ===== NHÓM 2: VẬN HÀNH =====
    private Long activeCampaigns;
    private BigDecimal dailyBurnRate;       // Tổng giảm giá / số ngày chiến dịch
    private Long budgetRiskCount;           // Số voucher ROI âm hoặc > 90% usage
    private Long conversionRate;            // % Đơn thành công / Tổng voucher phát hành

    // ===== TOP VOUCHERS =====
    private List<VoucherMetric> topByUsage;       // Top 10 dùng nhiều nhất
    private List<VoucherMetric> topByRevenue;     // Top 10 doanh thu cao nhất
    private List<VoucherMetric> topByROI;         // Top 10 ROI cao nhất

    // ===== PHÂN TÍCH THEO LOẠI =====
    private List<CategoryStat> byDiscountType;    // PERCENTAGE vs FIXED_AMOUNT
    private List<CategoryStat> byCouponCategory;  // Product / Order / Shipping
    private List<CategoryStat> byPaymentMethod;   // COD, VNPAY, BANK_TRANSFER

    // ===== PHỄU CHUYỂN ĐỔI =====
    private Long funnelIssued;     // Tổng phát hành
    private Long funnelSaved;      // Đã lưu vào ví
    private Long funnelApplied;    // Được áp dụng vào đơn
    private Long funnelCompleted;  // Đơn thành công (DELIVERED)

    // ===== PEAK HOURS HEATMAP (0-23h) =====
    private List<HourStat> usageByHour;

    // ===== RETENTION TRACKING =====
    private Long retainedUsersCount;        // Users dùng voucher rồi mua lại lần 2
    private Long voucherOnlyUsersCount;     // Users chỉ mua khi có voucher
    private BigDecimal retentionRate;       // retainedUsers / totalVoucherUsers * 100

    // ===== CẢNH BÁO =====
    private List<VoucherAlert> alerts;

    // ===== BẢNG CHI TIẾT =====
    private List<VoucherMetric> allVouchers; // Full table

    // ==================== INNER CLASSES ====================

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class VoucherMetric {
        private Long couponId;
        private String code;
        private String discountType;
        private String category;
        private String discountDisplay;    // "15%" hoặc "50.000₫"
        private Long usageCount;
        private Integer maxUsage;
        private BigDecimal usagePercent;
        private BigDecimal totalRevenue;
        private BigDecimal totalDiscount;
        private BigDecimal roi;
        private String status;
        private String startDate;
        private String endDate;
        private Long daysRemaining;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CategoryStat {
        private String label;
        private Long count;
        private BigDecimal totalDiscount;
        private BigDecimal totalRevenue;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class HourStat {
        private Integer hour;   // 0-23
        private Long usageCount;
        private BigDecimal revenue;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class VoucherAlert {
        private String type;        // "DANGER", "WARNING", "INFO"
        private String icon;        // "bi-exclamation-triangle", "bi-clock", "bi-lightbulb"
        private String title;
        private String message;
        private String couponCode;  // nullable - liên quan voucher cụ thể
    }
}
