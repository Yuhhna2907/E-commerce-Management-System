package com.codegym.smartphonemanagement.service.dashboard.seller;

import com.codegym.smartphonemanagement.repository.seller.ProductRepository;
import com.codegym.smartphonemanagement.repository.user.OrderRepository;
import com.codegym.smartphonemanagement.service.dashboard.DTO.DashboardDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public DashboardDTO getDashboardStats() {
        // 1. Lấy dữ liệu các con số tổng quát (Stat Cards)
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue(); // Doanh thu tổng (nhóm dùng)
        BigDecimal actualRevenue = orderRepository.calculateActualRevenue(); // Doanh thu thực (Duy dùng)
        long completedOrders = orderRepository.countCompletedOrders();
        long shippingOrders = orderRepository.countShippingOrders(); // Đơn đang giao
        long totalProducts = productRepository.countByActiveTrue();
        long lowStockCount = productRepository.countByActiveTrueAndStockLessThan(5);

        // 2. Lấy dữ liệu biểu đồ so sánh doanh thu 7 ngày
        List<Object[]> revenueComparisonData = orderRepository.getRevenueComparisonLast7Days();

        List<String> labels = new ArrayList<>();
        List<BigDecimal> actualValues = new ArrayList<>();
        List<BigDecimal> assumedValues = new ArrayList<>();

        for (Object[] row : revenueComparisonData) {
            labels.add(row[0].toString()); // Ngày
            actualValues.add(row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO); // Thực tế
            assumedValues.add(row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO); // Giả định
        }

        // 3. Lấy dữ liệu Top 5 sản phẩm bán chạy
        List<Object[]> topProducts = orderRepository.getTopSellingProducts(PageRequest.of(0, 5));

        // 4. Đóng gói vào DashboardDTO
        return DashboardDTO.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .actualRevenue(actualRevenue != null ? actualRevenue : BigDecimal.ZERO)
                .totalOrders(completedOrders)
                .shippingOrders(shippingOrders)
                .totalProducts(totalProducts)
                .lowStockCount(lowStockCount)

                // Dữ liệu biểu đồ doanh thu
                .revenueLabels(labels)
                .actualRevenueValues(actualValues)
                .assumedRevenueValues(assumedValues)

                // Giữ lại cái này để tránh lỗi nếu các bạn khác trong nhóm đang gọi
                .revenueValues(assumedValues)

                // Dữ liệu biểu đồ Top sản phẩm
                .topProductNames(topProducts.stream()
                        .map(row -> row[0].toString()).toList())
                .topProductSales(topProducts.stream()
                        .map(row -> ((Number) row[1]).longValue()).toList())
                .build();
    }
}