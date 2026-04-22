package com.codegym.smartphonemanagement.service.dashboard.seller;

import com.codegym.smartphonemanagement.repository.user.OrderRepository;
import com.codegym.smartphonemanagement.service.dashboard.DTO.OrderAnalyticsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderAnalyticsService {

    private final OrderRepository orderRepository;

    /**
     * Lấy báo cáo thống kê đơn hàng trong một khoảng thời gian
     */
    @Cacheable(value = "orderAnalytics", key = "#startDate.toString() + '_' + #endDate.toString()")
    public OrderAnalyticsDTO getAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating Order Analytics from {} to {}", startDate, endDate);

        long totalOrders = orderRepository.countOrdersInPeriod(startDate, endDate);
        
        // GMV
        BigDecimal gmv = orderRepository.sumGMVInPeriod(startDate, endDate);
        if (gmv == null) gmv = BigDecimal.ZERO;

        // Net Revenue
        BigDecimal netRevenue = orderRepository.sumNetRevenueInPeriod(startDate, endDate);
        if (netRevenue == null) netRevenue = BigDecimal.ZERO;

        // Discounts
        BigDecimal totalDiscount = orderRepository.sumTotalDiscountInPeriod(startDate, endDate);
        if (totalDiscount == null) totalDiscount = BigDecimal.ZERO;

        // AOV (Dựa trên Net Revenue và số đơn Delivered, để đơn giản tính trên GMV và total non-cancelled)
        BigDecimal aov = BigDecimal.ZERO;
        long nonCancelledOrders = getNonCancelledOrders(startDate, endDate);
        if (nonCancelledOrders > 0) {
            aov = gmv.divide(new BigDecimal(nonCancelledOrders), 2, RoundingMode.HALF_UP);
        }

        // Status Distribution
        List<Object[]> statusData = orderRepository.countOrdersByStatusInPeriod(startDate, endDate);
        Map<String, Long> statusDistribution = new HashMap<>();
        long cancelled = 0;
        long refunded = 0;
        
        for (Object[] row : statusData) {
            String status = row[0].toString();
            Long count = ((Number) row[1]).longValue();
            statusDistribution.put(status, count);
            if ("CANCELLED".equals(status)) cancelled += count;
            if ("REFUNDED".equals(status) || "PARTIAL_REFUNDED".equals(status)) refunded += count;
        }

        double cancelRate = totalOrders > 0 ? (double) cancelled / totalOrders * 100 : 0;
        double refundRate = totalOrders > 0 ? (double) refunded / totalOrders * 100 : 0;

        // Provinces
        List<Object[]> addressData = orderRepository.countOrdersByAddressInPeriod(startDate, endDate);
        Map<String, Long> tempProvinceMap = new HashMap<>();

        for (Object[] row : addressData) {
            String address = row[0] != null ? row[0].toString() : "";
            Long count = ((Number) row[1]).longValue();

            String province = "Chưa cập nhật";
            if (!address.trim().isEmpty()) {
                String[] parts = address.split(",");
                province = parts[parts.length - 1].trim();
            }

            tempProvinceMap.put(province, tempProvinceMap.getOrDefault(province, 0L) + count);
        }

        // Sort by count descending and get top 10
        Map<String, Long> provinceDistribution = new LinkedHashMap<>();
        tempProvinceMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> provinceDistribution.put(e.getKey(), e.getValue()));

        // Categories
        List<Object[]> categoryData = orderRepository.sumRevenueByCategoryInPeriod(startDate, endDate);
        Map<String, BigDecimal> categoryRevenue = new HashMap<>();
        for (Object[] row : categoryData) {
            String cat = row[0] != null ? row[0].toString() : "Khác";
            BigDecimal rev = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            categoryRevenue.put(cat, rev);
        }

        // Brands
        List<Object[]> brandData = orderRepository.sumRevenueByBrandInPeriod(startDate, endDate);
        Map<String, BigDecimal> brandRevenue = new HashMap<>();
        for (Object[] row : brandData) {
            String brand = row[0] != null ? row[0].toString() : "Khác";
            BigDecimal rev = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            brandRevenue.put(brand, rev);
        }

        // Trend
        List<Object[]> trendData = orderRepository.getRevenueTrendInPeriod(startDate, endDate);
        List<String> trendLabels = new ArrayList<>();
        List<BigDecimal> trendRevenues = new ArrayList<>();
        for (Object[] row : trendData) {
            trendLabels.add(row[0].toString());
            trendRevenues.add(new BigDecimal(row[1].toString()));
        }

        // Top Products
        List<Object[]> topProductData = orderRepository.sumTopSellingProductsInPeriod(startDate, endDate, PageRequest.of(0, 5));
        List<OrderAnalyticsDTO.ProductSalesDTO> topProducts = new ArrayList<>();
        for (Object[] row : topProductData) {
            topProducts.add(OrderAnalyticsDTO.ProductSalesDTO.builder()
                    .productName(row[0].toString())
                    .quantity(((Number) row[1]).longValue())
                    .revenue(new BigDecimal(row[2].toString()))
                    .build());
        }

        return OrderAnalyticsDTO.builder()
                .grossMerchandiseValue(gmv)
                .netRevenue(netRevenue)
                .averageOrderValue(aov)
                .totalDiscountAmount(totalDiscount)
                .totalOrders(totalOrders)
                .cancelledOrders(cancelled)
                .refundedOrders(refunded)
                .cancelRate(Math.round(cancelRate * 100.0) / 100.0)
                .refundRate(Math.round(refundRate * 100.0) / 100.0)
                .statusDistribution(statusDistribution)
                .provinceDistribution(provinceDistribution)
                .categoryRevenue(categoryRevenue)
                .brandRevenue(brandRevenue)
                .trendLabels(trendLabels)
                .trendRevenues(trendRevenues)
                .topProducts(topProducts)
                .build();
    }

    private long getNonCancelledOrders(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> statusData = orderRepository.countOrdersByStatusInPeriod(startDate, endDate);
        long count = 0;
        for (Object[] row : statusData) {
            String status = row[0].toString();
            if (!"CANCELLED".equals(status)) {
                count += ((Number) row[1]).longValue();
            }
        }
        return count;
    }
}
