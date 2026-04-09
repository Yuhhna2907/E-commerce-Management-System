package com.codegym.smartphonemanagement.service.dashboard.DTO;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter @Setter @Builder
public class DashboardDTO {
    // 4 con số tổng ở trên cùng
    private BigDecimal totalRevenue;
    private long totalOrders;
    private long totalProducts;
    private long lowStockCount;

    // Dữ liệu cho biểu đồ
    private List<Map<String, Object>> revenueChartData; // {date: '01/04', value: 5000000}
    private List<Map<String, Object>> topProductsData; // {name: 'iPhone 15', value: 20}
}