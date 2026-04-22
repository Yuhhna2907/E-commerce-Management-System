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
public class ProductAnalyticsDTO {
    // KPI Tổng quan (Header)
    private Long totalUniqueViews;
    private Long totalUnitsSold;
    private BigDecimal totalGrossRevenue;
    private Double averageConversionRate;
    private BigDecimal totalInventoryValue;
    private Long lowStockProductCount;

    // --- TAB 1: SALES ---
    private List<ProductMetricDTO> topSellingProducts;
    private List<ProductMetricDTO> lowSellingProducts;
    private List<ProductMetricDTO> topRevenueProducts;

    // --- TAB 2: STOCK ---
    private List<ProductMetricDTO> lowStockProducts;
    private List<ProductMetricDTO> highStockProducts; // Slow moving candidates
    private List<ProductMetricDTO> outOfStockProducts;

    // --- TAB 3: ENGAGEMENT ---
    private List<ProductMetricDTO> mostWishlistedProducts;
    private List<ProductMetricDTO> mostViewedProducts;
    private List<ProductMetricDTO> topRatedProducts;
    private List<ProductMetricDTO> mostQuestionedProducts;

    // --- TAB 4: CONVERSION ---
    private List<ProductMetricDTO> topConversionProducts;
    private List<ProductMetricDTO> lowConversionProducts;
    private BigDecimal averageOrderValue;
    private Double returnRate;
    private List<ProductMetricDTO> highAovProducts;
    private List<ProductMetricDTO> highReturnProducts;

    // --- TAB 5: COMPARISON ---
    private List<ProductMetricDTO> mostComparedProducts;
    private List<PairMetricDTO> topComparisonPairs;
    private List<ProductMetricDTO> savedToPurchasedConversion;

    // --- TAB 6: TRENDS ---
    private List<String> trendLabels;
    private List<BigDecimal> growthTrendData; // Percentage growth
    private List<ProductMetricDTO> fastGrowingProducts;
    private List<ProductMetricDTO> decliningProducts;
    private List<ProductMetricDTO> hotTrendProducts7Days;

    // --- TAB 7: SEGMENTS ---
    private Map<String, Long> customerSegmentDistribution; // New vs VIP
    private List<ProductMetricDTO> newCustomerFavorites;
    private List<ProductMetricDTO> vipCustomerFavorites;
    private List<ProductMetricDTO> highRepeatPurchaseProducts;
    private List<PairMetricDTO> topCrossSellOpportunities;

    // --- TAB 8: RISKS ---
    private List<ProductMetricDTO> oldStockProducts; // > 90 days
    private List<ProductMetricDTO> badReviewProducts;
    private List<ProductMetricDTO> highCancelRateProducts;
    private List<ProductMetricDTO> frequentOutOfStockProducts;

    // --- CATEGORY BREAKDOWN ---
    private List<CategoryDetailDTO> categoryDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductMetricDTO {
        private Long id;
        private String name;
        private String category;
        private Long value; // Can be views, sold, count, etc.
        private BigDecimal revenue;
        private Double percentage; // conversion rate, growth rate, etc.
        private Integer stock;
        private String status; // HOT, SLOW, etc.
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PairMetricDTO {
        private String product1;
        private String product2;
        private Long frequency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDetailDTO {
        private String name;
        private Long views;
        private Long sold;
        private BigDecimal revenue;
        private Double conversionRate;
    }
}
