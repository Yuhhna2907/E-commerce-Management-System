package com.codegym.smartphonemanagement.service.dashboard.seller;

import com.codegym.smartphonemanagement.repository.seller.ProductRepository;
import com.codegym.smartphonemanagement.repository.user.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1. Các con số tổng quát
        BigDecimal revenue = orderRepository.calculateTotalRevenue();
        stats.put("totalRevenue", revenue != null ? revenue : BigDecimal.ZERO);
        stats.put("totalOrders", orderRepository.countCompletedOrders());
        stats.put("totalProducts", productRepository.countByActiveTrue());
        stats.put("lowStockCount", productRepository.countByActiveTrueAndStockLessThan(5));

        // 2. Dữ liệu biểu đồ doanh thu 7 ngày
        List<Object[]> revenueData = orderRepository.getRevenueLast7Days();
        // Dùng .stream().map(...).toList() (Java 16+) hoặc .collect(Collectors.toList())
        stats.put("revenueLabels", revenueData.stream().map(row -> row[0].toString()).toList());
        stats.put("revenueValues", revenueData.stream().map(row -> row[1]).toList());

        // 3. Dữ liệu Top 5 sản phẩm bán chạy (Cần PageRequest từ Spring Data Domain)
        List<Object[]> topProducts = orderRepository.getTopSellingProducts(PageRequest.of(0, 5));
        stats.put("topProductNames", topProducts.stream().map(row -> row[0]).toList());
        stats.put("topProductSales", topProducts.stream().map(row -> row[1]).toList());

        return stats;
    }
}