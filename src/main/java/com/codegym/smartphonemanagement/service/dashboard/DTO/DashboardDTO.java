package com.codegym.smartphonemanagement.service.dashboard.DTO;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardDTO {
    // --- 4 CON SỐ TỔNG QUÁT (Stat Cards) ---
    private BigDecimal totalRevenue;    // Doanh thu tổng (nhóm đang dùng)
    private BigDecimal actualRevenue;   // Doanh thu THỰC TẾ (Đã giao thành công)
    private long totalOrders;           // Tổng đơn thành công
    private long shippingOrders;        // Đơn hàng ĐANG GIAO (Mới thêm)
    private long totalProducts;
    private long lowStockCount;

    // --- DỮ LIỆU BIỂU ĐỒ DOANH THU (Line Chart) ---
    private List<String> revenueLabels;       // Danh sách các ngày: "2026-04-08", "2026-04-09"...
    private List<BigDecimal> actualRevenueValues;  // Cột doanh thu THỰC (xanh lá)
    private List<BigDecimal> assumedRevenueValues; // Cột doanh thu GIẢ ĐỊNH (vàng)

    // Giữ lại để tránh lỗi code cũ nếu có chỗ đang dùng
    private List<BigDecimal> revenueValues;

    // --- DỮ LIỆU BIỂU ĐỒ TRÒN (Top 5 Sản phẩm) ---
    private List<String> topProductNames;
    private List<Long> topProductSales;
}