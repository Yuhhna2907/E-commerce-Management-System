package com.codegym.smartphonemanagement.service.recommendation;

import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.model.ProductRecommendation;
import com.codegym.smartphonemanagement.repository.seller.ProductRepository;
import com.codegym.smartphonemanagement.repository.user.OrderItemRepository;
import com.codegym.smartphonemanagement.repository.user.ProductRecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service xử lý logic gợi ý sản phẩm dựa trên co-purchase patterns
 * 
 * Yêu cầu: 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.3, 6.4
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {
    
    private final ProductRecommendationRepository recommendationRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    
    // Constants theo yêu cầu - ADJUSTED FOR DEVELOPMENT
    private static final BigDecimal MIN_FREQUENCY = new BigDecimal("0.01"); // 1% (giảm từ 5%)
    private static final int MIN_CO_PURCHASE_COUNT = 2; // 2 lần (giảm từ 10 lần)
    private static final int MAX_RECOMMENDATIONS = 6;
    private static final int MIN_RECOMMENDATIONS = 4;
    
    /**
     * Lấy danh sách gợi ý cho một sản phẩm với caching
     * 
     * Yêu cầu: 5.2, 5.3, 5.4, 5.5, 6.4
     * 
     * @param productId ID của sản phẩm cần gợi ý
     * @return Danh sách sản phẩm được gợi ý (4-6 sản phẩm)
     */
    @Cacheable(value = "productRecommendations", key = "#productId")
    @Transactional(readOnly = true)
    public List<Product> getRecommendations(Long productId) {
        validateProductId(productId);
        
        log.info("Getting recommendations for product ID: {}", productId);
        
        // Lấy danh sách recommendations từ database (với JOIN FETCH để avoid N+1)
        List<ProductRecommendation> recommendations = recommendationRepository
                .findValidRecommendationsByProductId(productId, MIN_FREQUENCY, MIN_CO_PURCHASE_COUNT);
        
        if (recommendations.isEmpty()) {
            log.info("No valid recommendations found for product ID: {}", productId);
            return new ArrayList<>();
        }
        
        // Lấy từ 4-6 sản phẩm được gợi ý
        int limit = Math.min(MAX_RECOMMENDATIONS, recommendations.size());
        limit = Math.max(MIN_RECOMMENDATIONS, limit);
        
        List<Product> recommendedProducts = recommendations.stream()
                .limit(limit)
                .map(ProductRecommendation::getRecommendedProduct)
                .filter(product -> product.getActive() && product.getStock() > 0)
                .collect(Collectors.toList());
        
        log.info("Returning {} recommendations for product ID: {}", recommendedProducts.size(), productId);
        return recommendedProducts;
    }
    
    /**
     * Tính toán co-purchase counts cho một sản phẩm
     * 
     * Yêu cầu: 5.1, 6.1
     * 
     * @param productId ID của sản phẩm cần phân tích
     * @return Map với key là product ID và value là số lần mua chung
     */
    @Transactional(readOnly = true)
    public Map<Long, Integer> calculateCoPurchaseCounts(Long productId) {
        log.debug("Calculating co-purchase counts for product ID: {}", productId);
        
        // Lấy danh sách sản phẩm được mua cùng từ OrderItemRepository
        List<Object[]> coPurchaseData = orderItemRepository.findCoPurchasedProducts(productId);
        
        // Convert sang Map<ProductId, Count>
        Map<Long, Integer> coPurchaseCounts = coPurchaseData.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],           // product_id
                        row -> ((Long) row[1]).intValue() // count
                ));
        
        log.debug("Found {} co-purchased products for product ID: {}", coPurchaseCounts.size(), productId);
        return coPurchaseCounts;
    }
    
    /**
     * Build recommendations cho một sản phẩm cụ thể
     * 
     * Yêu cầu: 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.3, 6.4
     * 
     * @param productId ID của sản phẩm cần build recommendations
     */
    @CacheEvict(value = "productRecommendations", key = "#productId")
    @Transactional
    public void buildRecommendationsForProduct(Long productId) {
        validateProductId(productId);
        
        log.info("Building recommendations for product ID: {}", productId);
        
        // Kiểm tra sản phẩm có tồn tại không
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        
        if (!product.getActive()) {
            log.warn("Product ID {} is not active, skipping recommendation build", productId);
            return;
        }
        
        // Đếm tổng số đơn hàng chứa sản phẩm này
        int totalOrders = orderItemRepository.countDistinctOrdersByProductId(productId);
        
        if (totalOrders == 0) {
            log.info("Product ID {} has no orders, skipping recommendation build", productId);
            return;
        }
        
        log.debug("Product ID {} appears in {} orders", productId, totalOrders);
        
        // Tính co-purchase counts
        Map<Long, Integer> coPurchaseCounts = calculateCoPurchaseCounts(productId);
        
        if (coPurchaseCounts.isEmpty()) {
            log.info("No co-purchase patterns found for product ID: {}", productId);
            return;
        }
        
        // Xóa recommendations cũ cho sản phẩm này
        recommendationRepository.deleteByProductId(productId);
        log.debug("Deleted old recommendations for product ID: {}", productId);
        
        // Tạo recommendations mới
        List<ProductRecommendation> newRecommendations = buildRecommendationList(
                product, coPurchaseCounts, totalOrders);
        
        // Lưu vào database
        if (!newRecommendations.isEmpty()) {
            recommendationRepository.saveAll(newRecommendations);
            log.info("Saved {} recommendations for product ID: {}", newRecommendations.size(), productId);
        } else {
            log.info("No valid recommendations to save for product ID: {}", productId);
        }
    }
    
    /**
     * Build danh sách recommendations từ co-purchase data
     */
    private List<ProductRecommendation> buildRecommendationList(
            Product product, Map<Long, Integer> coPurchaseCounts, int totalOrders) {
        
        List<ProductRecommendation> recommendations = new ArrayList<>();
        
        for (Map.Entry<Long, Integer> entry : coPurchaseCounts.entrySet()) {
            Long recommendedProductId = entry.getKey();
            Integer coPurchaseCount = entry.getValue();
            
            // Lọc theo min count >= 2 (Yêu cầu 6.3 - ADJUSTED FOR DEVELOPMENT)
            if (coPurchaseCount < MIN_CO_PURCHASE_COUNT) {
                continue;
            }
            
            // Tính frequency = co-purchase count / total orders (Yêu cầu 6.1)
            BigDecimal frequency = BigDecimal.valueOf(coPurchaseCount)
                    .divide(BigDecimal.valueOf(totalOrders), 4, RoundingMode.HALF_UP);
            
            // Lọc theo min frequency >= 1% (Yêu cầu 5.5 - ADJUSTED FOR DEVELOPMENT)
            if (frequency.compareTo(MIN_FREQUENCY) < 0) {
                continue;
            }
            
            // Lấy recommended product
            Product recommendedProduct = productRepository.findById(recommendedProductId)
                    .orElse(null);
            
            if (recommendedProduct == null || !recommendedProduct.getActive()) {
                continue;
            }
            
            // Tạo ProductRecommendation entity
            ProductRecommendation recommendation = ProductRecommendation.builder()
                    .product(product)
                    .recommendedProduct(recommendedProduct)
                    .coPurchaseCount(coPurchaseCount)
                    .coPurchaseFrequency(frequency)
                    .build();
            
            recommendations.add(recommendation);
            
            log.debug("Created recommendation: Product {} -> Product {} (count: {}, frequency: {}%)",
                    product.getId(), recommendedProductId, coPurchaseCount, 
                    frequency.multiply(BigDecimal.valueOf(100)));
        }
        
        // Sắp xếp theo frequency giảm dần (Yêu cầu 6.4)
        recommendations.sort((r1, r2) -> 
                r2.getCoPurchaseFrequency().compareTo(r1.getCoPurchaseFrequency()));
        
        return recommendations;
    }
    
    /**
     * Kiểm tra xem sản phẩm có recommendations hợp lệ không
     * 
     * @param productId ID của sản phẩm
     * @return true nếu có ít nhất 1 recommendation hợp lệ
     */
    @Transactional(readOnly = true)
    public boolean hasRecommendations(Long productId) {
        validateProductId(productId);
        return recommendationRepository.hasValidRecommendations(productId);
    }
    
    /**
     * Đếm số lượng recommendations hợp lệ cho một sản phẩm
     * 
     * @param productId ID của sản phẩm
     * @return Số lượng recommendations hợp lệ
     */
    @Transactional(readOnly = true)
    public long countRecommendations(Long productId) {
        validateProductId(productId);
        return recommendationRepository.countValidRecommendations(productId);
    }
    
    /**
     * Lấy thống kê tổng quan về recommendation system
     * 
     * @return Map chứa các thống kê
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRecommendationStats() {
        Object[] stats = recommendationRepository.getRecommendationStats();
        
        return Map.of(
                "totalRecommendations", stats[0],
                "validRecommendations", stats[1],
                "uniqueProducts", stats[2]
        );
    }
    
    /**
     * Scheduled job để rebuild recommendations cho tất cả sản phẩm active
     * Chạy hàng ngày lúc 2:00 AM
     * 
     * Yêu cầu: 6.2
     * 
     * Cron expression: 0 0 2 * * ? (giây phút giờ ngày tháng thứ)
     * - 0: giây thứ 0
     * - 0: phút thứ 0
     * - 2: giờ thứ 2 (2:00 AM)
     * - *: mọi ngày trong tháng
     * - *: mọi tháng
     * - ?: không quan tâm thứ trong tuần
     */
    @Scheduled(cron = "${app.scheduling.recommendation-rebuild-cron:0 0 2 * * ?}")
    public void rebuildRecommendations() {
        log.info("========================================");
        log.info("Starting scheduled recommendation rebuild job");
        log.info("========================================");
        
        long startTime = System.currentTimeMillis();
        RebuildResult result = new RebuildResult();
        
        try {
            // Lấy IDs của tất cả sản phẩm active (chỉ load IDs, không load full entities)
            List<Long> activeProductIds = productRepository.findAll().stream()
                    .filter(Product::getActive)
                    .map(Product::getId)
                    .collect(Collectors.toList());
            
            log.info("Found {} active products to process", activeProductIds.size());
            
            if (activeProductIds.isEmpty()) {
                log.warn("No active products found, skipping recommendation rebuild");
                return;
            }
            
            // Rebuild recommendations cho từng sản phẩm
            for (Long productId : activeProductIds) {
                try {
                    processProductRecommendation(productId, result);
                } catch (Exception e) {
                    result.failureCount++;
                    log.error("Failed to rebuild recommendations for product ID: {} - Error: {}", 
                            productId, e.getMessage(), e);
                    // Continue với sản phẩm tiếp theo thay vì dừng toàn bộ job
                }
            }
            
            logRebuildSummary(result, activeProductIds.size(), startTime);
            
        } catch (Exception e) {
            log.error("========================================");
            log.error("CRITICAL ERROR in recommendation rebuild job: {}", e.getMessage(), e);
            log.error("========================================");
            throw e; // Re-throw để Spring có thể log và handle
        }
    }
    
    /**
     * Process recommendation rebuild cho một sản phẩm
     */
    @Transactional
    private void processProductRecommendation(Long productId, RebuildResult result) {
        log.debug("Processing product ID: {}", productId);
        
        // Kiểm tra xem sản phẩm có đơn hàng không
        int totalOrders = orderItemRepository.countDistinctOrdersByProductId(productId);
        
        if (totalOrders == 0) {
            log.debug("Product ID {} has no orders, skipping", productId);
            result.skippedCount++;
            return;
        }
        
        // Build recommendations
        buildRecommendationsForProduct(productId);
        result.successCount++;
        
        log.debug("Successfully rebuilt recommendations for product ID: {}", productId);
    }
    
    /**
     * Log rebuild summary
     */
    private void logRebuildSummary(RebuildResult result, int totalProducts, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        
        log.info("========================================");
        log.info("Recommendation rebuild job completed");
        log.info("Total products processed: {}", totalProducts);
        log.info("Success: {}", result.successCount);
        log.info("Skipped (no orders): {}", result.skippedCount);
        log.info("Failed: {}", result.failureCount);
        log.info("Duration: {} ms ({} seconds)", duration, duration / 1000);
        log.info("========================================");
    }
    
    /**
     * Helper class để track rebuild results
     */
    private static class RebuildResult {
        int successCount = 0;
        int failureCount = 0;
        int skippedCount = 0;
    }
    
    /**
     * Manual trigger để rebuild recommendations cho tất cả sản phẩm
     * Có thể được gọi từ admin controller
     * 
     * @return Map chứa kết quả rebuild
     */
    @CacheEvict(value = "productRecommendations", allEntries = true)
    public Map<String, Object> rebuildAllRecommendationsManually() {
        log.info("Manual recommendation rebuild triggered");
        
        long startTime = System.currentTimeMillis();
        RebuildResult result = new RebuildResult();
        
        // Lấy IDs của tất cả sản phẩm active
        List<Long> activeProductIds = productRepository.findAll().stream()
                .filter(Product::getActive)
                .map(Product::getId)
                .collect(Collectors.toList());
        
        for (Long productId : activeProductIds) {
            try {
                processProductRecommendation(productId, result);
            } catch (Exception e) {
                result.failureCount++;
                log.error("Failed to rebuild recommendations for product ID: {}", productId, e);
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        return Map.of(
                "totalProducts", activeProductIds.size(),
                "success", result.successCount,
                "skipped", result.skippedCount,
                "failed", result.failureCount,
                "durationMs", duration
        );
    }
    
    // ==================== Validation Helper Methods ====================
    
    /**
     * Validate product ID
     */
    private void validateProductId(Long productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Product ID không hợp lệ: " + productId);
        }
    }
}
