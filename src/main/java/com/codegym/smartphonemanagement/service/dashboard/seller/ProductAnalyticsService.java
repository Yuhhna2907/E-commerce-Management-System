package com.codegym.smartphonemanagement.service.dashboard.seller;

import com.codegym.smartphonemanagement.model.Category;
import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.repository.user.CategoryRepository;
import com.codegym.smartphonemanagement.repository.seller.ProductRepository;
import com.codegym.smartphonemanagement.repository.user.*;
import com.codegym.smartphonemanagement.service.dashboard.DTO.ProductAnalyticsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductAnalyticsService {

    private final ProductRepository productRepository;
    private final RecentlyViewedRepository recentlyViewedRepository;
    private final OrderItemRepository orderItemRepository;
    private final CategoryRepository categoryRepository;
    private final WishlistRepository wishlistRepository;
    private final CompareRepository compareRepository;
    private final ProductQuestionRepository productQuestionRepository;
    private final ReviewRepository reviewRepository;

    @Cacheable(value = "advancedProductAnalytics", key = "#startDate.toString() + '_' + #endDate.toString()")
    public ProductAnalyticsDTO getAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating Advanced Product Analytics from {} to {}", startDate, endDate);

        // Fetch all active products for mapping
        Map<Long, Product> productMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // --- TAB 1: SALES ---
        List<ProductAnalyticsDTO.ProductMetricDTO> topSelling = mapToMetric(
                orderItemRepository.sumTopSellingProductsInPeriod(startDate, endDate, PageRequest.of(0, 10)), 
                productMap, 1L);
        List<ProductAnalyticsDTO.ProductMetricDTO> topRevenue = mapToMetric(
                orderItemRepository.sumSalesByProductInPeriod(startDate, endDate), 
                productMap, 2L);
        topRevenue.sort((a, b) -> b.getRevenue().compareTo(a.getRevenue()));

        // --- TAB 2: STOCK ---
        List<ProductAnalyticsDTO.ProductMetricDTO> lowStock = productRepository.findByActiveTrueAndStockLessThan(10).stream()
                .map(p -> mapToSingleMetric(p, (long) p.getStock())).collect(Collectors.toList());
        List<ProductAnalyticsDTO.ProductMetricDTO> highStock = productRepository.findByActiveTrueAndStockGreaterThanOrderByStockDesc(50, PageRequest.of(0, 10)).stream()
                .map(p -> mapToSingleMetric(p, (long) p.getStock())).collect(Collectors.toList());
        List<ProductAnalyticsDTO.ProductMetricDTO> outOfStock = productRepository.findByActiveTrueAndStockEquals(0).stream()
                .map(p -> mapToSingleMetric(p, 0L)).collect(Collectors.toList());

        // --- TAB 3: ENGAGEMENT ---
        List<ProductAnalyticsDTO.ProductMetricDTO> mostWishlisted = mapToMetric(wishlistRepository.countByProduct(), productMap, 1L);
        List<ProductAnalyticsDTO.ProductMetricDTO> mostViewed = mapToMetric(recentlyViewedRepository.countUniqueViewsByProductInPeriod(startDate, endDate), productMap, 1L);
        List<ProductAnalyticsDTO.ProductMetricDTO> topRated = mapToMetric(reviewRepository.countRatingsByProduct(), productMap, 1L);
        List<ProductAnalyticsDTO.ProductMetricDTO> mostQuestioned = mapToMetric(productQuestionRepository.countQuestionsByProductInPeriod(startDate, endDate), productMap, 1L);

        // --- TAB 4: CONVERSION ---
        Map<Long, Long> viewsMap = mostViewed.stream().collect(Collectors.toMap(ProductAnalyticsDTO.ProductMetricDTO::getId, ProductAnalyticsDTO.ProductMetricDTO::getValue));
        List<ProductAnalyticsDTO.ProductMetricDTO> conversionStats = topSelling.stream()
                .peek(s -> {
                    long views = viewsMap.getOrDefault(s.getId(), 0L);
                    double cr = views > 0 ? (double) s.getValue() / views * 100 : 0;
                    s.setPercentage(Math.round(cr * 100.0) / 100.0);
                })
                .sorted((a, b) -> Double.compare(b.getPercentage(), a.getPercentage()))
                .toList();

        // --- TAB 5: COMPARISON ---
        List<ProductAnalyticsDTO.ProductMetricDTO> mostCompared = mapToMetric(compareRepository.countByProduct(), productMap, 1L);
        List<ProductAnalyticsDTO.PairMetricDTO> topPairs = compareRepository.findTopComparePairs(PageRequest.of(0, 10)).stream()
                .map(r -> new ProductAnalyticsDTO.PairMetricDTO(r[0].toString(), r[1].toString(), ((Number) r[2]).longValue()))
                .collect(Collectors.toList());

        // --- TAB 6: RISKS ---
        List<ProductAnalyticsDTO.ProductMetricDTO> agedInventory = productRepository.findAgedInventory(LocalDateTime.now().minusDays(90)).stream()
                .map(p -> mapToSingleMetric(p, (long) p.getStock())).collect(Collectors.toList());
        List<ProductAnalyticsDTO.ProductMetricDTO> negativeReviews = mapToMetric(reviewRepository.findProductsWithNegativeReviews(), productMap, 1L);

        // --- TAB 7: TRENDS ---
        List<ProductAnalyticsDTO.ProductMetricDTO> hotTrend = mapToMetric(recentlyViewedRepository.getHotTrendProducts(startDate, endDate, PageRequest.of(0, 10)), productMap, 1L);
        List<Object[]> rawGrowth = orderItemRepository.getSalesGrowthTrend(startDate, endDate);
        List<String> trendLabels = new ArrayList<>();
        List<BigDecimal> growthTrendData = new ArrayList<>();
        for (Object[] row : rawGrowth) {
            trendLabels.add(row[0] != null ? row[0].toString() : "");
            growthTrendData.add(row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO);
        }

        // --- TAB 8: SEGMENTS ---
        List<ProductAnalyticsDTO.ProductMetricDTO> vipFavs = mapToMetric(orderItemRepository.findVipCustomerFavorites(5000, PageRequest.of(0, 5)), productMap, 1L);
        List<ProductAnalyticsDTO.ProductMetricDTO> newFavs = mapToMetric(orderItemRepository.findNewCustomerFavorites(1000, PageRequest.of(0, 5)), productMap, 1L);
        List<ProductAnalyticsDTO.ProductMetricDTO> repeatPurchases = mapToMetric(orderItemRepository.findRepeatPurchaseProducts(), productMap, 1L);
        List<ProductAnalyticsDTO.PairMetricDTO> topCrossSell = orderItemRepository.findTopCrossSellPairs(PageRequest.of(0, 5)).stream()
                .map(r -> new ProductAnalyticsDTO.PairMetricDTO(r[0].toString(), r[1].toString(), ((Number) r[2]).longValue()))
                .collect(Collectors.toList());

        // Category Breakdown
        List<Category> allCategories = categoryRepository.findAll();
        List<Object[]> catSales = orderItemRepository.sumSalesByCategoryInPeriod(startDate, endDate);
        Map<Long, Object[]> catSalesMap = catSales.stream().collect(Collectors.toMap(r -> (Long) r[0], r -> r));

        List<ProductAnalyticsDTO.CategoryDetailDTO> catDetails = allCategories.stream().map(c -> {
            Object[] s = catSalesMap.get(c.getId());
            return ProductAnalyticsDTO.CategoryDetailDTO.builder()
                    .name(c.getName())
                    .sold(s != null ? ((Number) s[1]).longValue() : 0L)
                    .revenue(s != null ? new BigDecimal(s[2].toString()) : BigDecimal.ZERO)
                    .build();
        }).collect(Collectors.toList());

        return ProductAnalyticsDTO.builder()
                .totalUniqueViews(!mostViewed.isEmpty() ? mostViewed.stream().mapToLong(ProductAnalyticsDTO.ProductMetricDTO::getValue).sum() : 0L)
                .totalUnitsSold(topSelling != null && !topSelling.isEmpty() ? topSelling.stream().mapToLong(ProductAnalyticsDTO.ProductMetricDTO::getValue).sum() : 0L)
                .totalGrossRevenue(topRevenue != null && !topRevenue.isEmpty() ? topRevenue.stream().map(ProductAnalyticsDTO.ProductMetricDTO::getRevenue).reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO)
                .topSellingProducts(topSelling != null ? topSelling : new ArrayList<>())
                .lowSellingProducts(new ArrayList<>()) // Not implemented yet
                .topRevenueProducts(topRevenue != null ? topRevenue : new ArrayList<>())
                .lowStockProducts(lowStock != null ? lowStock : new ArrayList<>())
                .highStockProducts(highStock != null ? highStock : new ArrayList<>())
                .outOfStockProducts(outOfStock != null ? outOfStock : new ArrayList<>())
                .mostWishlistedProducts(mostWishlisted != null ? mostWishlisted : new ArrayList<>())
                .mostViewedProducts(mostViewed != null ? mostViewed : new ArrayList<>())
                .topRatedProducts(topRated != null ? topRated : new ArrayList<>())
                .mostQuestionedProducts(mostQuestioned != null ? mostQuestioned : new ArrayList<>())
                .topConversionProducts(conversionStats != null ? conversionStats.stream().limit(10).collect(Collectors.toList()) : new ArrayList<>())
                .lowConversionProducts(conversionStats != null ? conversionStats.stream().filter(p -> p.getPercentage() != null && p.getPercentage() < 2).limit(10).collect(Collectors.toList()) : new ArrayList<>())
                .mostComparedProducts(mostCompared != null ? mostCompared : new ArrayList<>())
                .topComparisonPairs(topPairs != null ? topPairs : new ArrayList<>())
                .savedToPurchasedConversion(new ArrayList<>()) // Not implemented yet
                .trendLabels(trendLabels)
                .growthTrendData(growthTrendData)
                .hotTrendProducts7Days(hotTrend)
                .vipCustomerFavorites(vipFavs)
                .newCustomerFavorites(newFavs)
                .highRepeatPurchaseProducts(repeatPurchases)
                .topCrossSellOpportunities(topCrossSell)
                .oldStockProducts(agedInventory != null ? agedInventory : new ArrayList<>())
                .badReviewProducts(negativeReviews != null ? negativeReviews : new ArrayList<>())
                .categoryDetails(catDetails != null ? catDetails : new ArrayList<>())
                .totalInventoryValue(productRepository.calculateTotalInventoryValue() != null ? productRepository.calculateTotalInventoryValue() : BigDecimal.ZERO)
                .lowStockProductCount(productRepository.countByActiveTrueAndStockLessThan(10))
                .build();
    }

    private List<ProductAnalyticsDTO.ProductMetricDTO> mapToMetric(List<Object[]> data, Map<Long, Product> productMap, long valueIdx) {
        List<ProductAnalyticsDTO.ProductMetricDTO> list = new ArrayList<>();
        for (Object[] row : data) {
            Long id = (Long) row[0];
            Product p = productMap.get(id);
            if (p != null) {
                list.add(ProductAnalyticsDTO.ProductMetricDTO.builder()
                        .id(id)
                        .name(p.getName())
                        .category(p.getCategory() != null ? p.getCategory().getName() : "Khác")
                        .value(((Number) row[1]).longValue())
                        .revenue(row.length > 2 ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO)
                        .stock(p.getStock())
                        .build());
            }
        }
        return list;
    }

    private ProductAnalyticsDTO.ProductMetricDTO mapToSingleMetric(Product p, Long val) {
        return ProductAnalyticsDTO.ProductMetricDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .category(p.getCategory() != null ? p.getCategory().getName() : "Khác")
                .value(val)
                .stock(p.getStock())
                .revenue(BigDecimal.ZERO)
                .build();
    }
}
