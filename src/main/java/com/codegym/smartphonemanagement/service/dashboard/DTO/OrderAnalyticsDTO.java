package com.codegym.smartphonemanagement.service.dashboard.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderAnalyticsDTO {
    // --- KHUYẾN MÃI & TÀI CHÍNH ---
    private BigDecimal grossMerchandiseValue; // GMV - Tổng tiền mọi đơn (không tính Cancelled)
    private BigDecimal netRevenue; // Doanh thu thuần (Chỉ tính DELIVERED)
    private BigDecimal averageOrderValue; // AOV
    private BigDecimal totalDiscountAmount; // Tiền giảm giá

    // --- VẬN HÀNH ---
    private long totalOrders;
    private long cancelledOrders;
    private long refundedOrders;
    
    private double cancelRate; // Tỷ lệ hủy %
    private double refundRate; // Tỷ lệ hoàn %

    // Thời gian xử lý trung bình (tuỳ chọn - nếu tính được)
    // private String averageProcessingTime;

    // --- PHÂN BỔ ---
    // Doanh thu theo ngày (xu hướng)
    private List<String> trendLabels;
    private List<BigDecimal> trendRevenues;

    // Theo trạng thái
    private Map<String, Long> statusDistribution;
    
    // Theo phân loại sản phẩm
    private Map<String, BigDecimal> categoryRevenue;
    private Map<String, BigDecimal> brandRevenue;

    // Theo Tỉnh / Thành phố
    private Map<String, Long> provinceDistribution;

    // Top Sản phẩm
    private List<ProductSalesDTO> topProducts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSalesDTO {
        private String productName;
        private long quantity;
        private BigDecimal revenue;
    }
}
