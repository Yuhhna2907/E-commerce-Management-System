package com.codegym.smartphonemanagement.service.analytics;

import com.codegym.smartphonemanagement.model.PinnedProduct;
import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.model.dto.PinnedProductOrderDTO;
import com.codegym.smartphonemanagement.model.dto.RebuildResultDTO;
import com.codegym.smartphonemanagement.model.dto.RecommendationStatusDTO;
import com.codegym.smartphonemanagement.repository.seller.ProductRepository;
import com.codegym.smartphonemanagement.repository.user.PinnedProductRepository;
import com.codegym.smartphonemanagement.repository.user.ProductRecommendationRepository;
import com.codegym.smartphonemanagement.service.recommendation.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service for Recommendation Management
 * Provides admin controls for recommendation engine and pinned products
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationManagementService {
    
    private final RecommendationService recommendationService;
    private final ProductRecommendationRepository recommendationRepository;
    private final PinnedProductRepository pinnedProductRepository;
    private final ProductRepository productRepository;
    
    // Track rebuild status
    private final AtomicBoolean isRebuilding = new AtomicBoolean(false);
    private final AtomicReference<LocalDateTime> lastRebuildTime = new AtomicReference<>();
    private final AtomicReference<RebuildResultDTO> lastRebuildResult = new AtomicReference<>();
    
    private static final int MAX_PINNED_PRODUCTS = 10;
    
    /**
     * Get recommendation engine status for dashboard widget
     * Results are cached for 5 minutes
     * 
     * @return Recommendation engine status
     */
    @Cacheable(value = "recommendationWidget", key = "'status'")
    public RecommendationStatusDTO getRecommendationStatus() {
        log.debug("Fetching recommendation engine status");
        
        // Get total recommendations count
        long totalCount = recommendationRepository.count();
        
        // Determine health status
        String healthStatus = determineHealthStatus(totalCount);
        
        // Get last rebuild time
        LocalDateTime lastRebuild = lastRebuildTime.get();
        
        RecommendationStatusDTO status = RecommendationStatusDTO.builder()
            .lastRebuildTimestamp(lastRebuild)
            .totalRecommendationsCount((int) totalCount)
            .engineHealthStatus(healthStatus)
            .isRebuilding(isRebuilding.get())
            .build();
        
        log.info("Recommendation engine status - Total: {}, Health: {}, Rebuilding: {}", 
                 totalCount, healthStatus, isRebuilding.get());
        
        return status;
    }
    
    /**
     * Determine health status based on recommendation count
     */
    private String determineHealthStatus(long totalCount) {
        if (totalCount == 0) {
            return "error";
        } else if (totalCount < 100) {
            return "warning";
        } else {
            return "healthy";
        }
    }
    
    /**
     * Trigger async rebuild of recommendation matrix
     * Clears cache and rebuilds all recommendations
     * 
     * @return CompletableFuture with rebuild result
     */
    @Async
    @CacheEvict(value = {"recommendationWidget", "productRecommendations"}, allEntries = true)
    public CompletableFuture<RebuildResultDTO> rebuildRecommendationMatrix() {
        if (!isRebuilding.compareAndSet(false, true)) {
            log.warn("Recommendation rebuild already in progress");
            return CompletableFuture.completedFuture(
                RebuildResultDTO.builder()
                    .success(false)
                    .message("Rebuild đang được thực hiện, vui lòng đợi")
                    .build()
            );
        }
        
        log.info("========================================");
        log.info("Starting manual recommendation rebuild");
        log.info("========================================");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Call existing rebuild method
            Map<String, Object> result = recommendationService.rebuildAllRecommendationsManually();
            
            long duration = System.currentTimeMillis() - startTime;
            LocalDateTime completedAt = LocalDateTime.now();
            
            RebuildResultDTO resultDTO = RebuildResultDTO.builder()
                .success(true)
                .message("Rebuild hoàn tất thành công")
                .totalProducts((Integer) result.get("totalProducts"))
                .successCount((Integer) result.get("success"))
                .skippedCount((Integer) result.get("skipped"))
                .failedCount((Integer) result.get("failed"))
                .durationMs(duration)
                .completedAt(completedAt)
                .build();
            
            // Update last rebuild time and result
            lastRebuildTime.set(completedAt);
            lastRebuildResult.set(resultDTO);
            
            log.info("========================================");
            log.info("Manual recommendation rebuild completed successfully");
            log.info("Duration: {} ms", duration);
            log.info("========================================");
            
            return CompletableFuture.completedFuture(resultDTO);
            
        } catch (Exception e) {
            log.error("========================================");
            log.error("Manual recommendation rebuild failed: {}", e.getMessage(), e);
            log.error("========================================");
            
            RebuildResultDTO errorResult = RebuildResultDTO.builder()
                .success(false)
                .message("Rebuild thất bại: " + e.getMessage())
                .durationMs(System.currentTimeMillis() - startTime)
                .completedAt(LocalDateTime.now())
                .build();
            
            lastRebuildResult.set(errorResult);
            
            return CompletableFuture.completedFuture(errorResult);
            
        } finally {
            isRebuilding.set(false);
        }
    }
    
    /**
     * Get rebuild progress/status
     * 
     * @return Map with rebuild status information
     */
    public Map<String, Object> getRebuildProgress() {
        RebuildResultDTO lastResult = lastRebuildResult.get();
        
        return Map.of(
            "isRebuilding", isRebuilding.get(),
            "lastRebuildTime", lastRebuildTime.get() != null ? lastRebuildTime.get() : "Chưa có",
            "lastResult", lastResult != null ? lastResult : "Chưa có"
        );
    }
    
    // ==================== Pinned Products Management ====================
    
    /**
     * Pin a product to homepage
     * 
     * @param productId Product to pin
     * @param displayOrder Display order (1-10)
     * @param pinnedByUser Admin user who pinned the product
     * @return Pinned product entity
     */
    @Transactional
    public PinnedProduct pinProduct(Long productId, int displayOrder, User pinnedByUser) {
        log.info("Pinning product {} with display order {}", productId, displayOrder);
        
        // Validate product exists and is active
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại: " + productId));
        
        if (!product.getActive()) {
            throw new IllegalArgumentException("Không thể ghim sản phẩm không hoạt động");
        }
        
        // Check if already pinned
        if (pinnedProductRepository.existsByProductIdAndActiveTrue(productId)) {
            throw new IllegalArgumentException("Sản phẩm đã được ghim");
        }
        
        // Check maximum limit
        int currentCount = pinnedProductRepository.countByActiveTrue();
        if (currentCount >= MAX_PINNED_PRODUCTS) {
            throw new IllegalArgumentException(
                String.format("Đã đạt giới hạn tối đa %d sản phẩm ghim", MAX_PINNED_PRODUCTS));
        }
        
        // Create pinned product
        PinnedProduct pinnedProduct = PinnedProduct.builder()
            .product(product)
            .displayOrder(displayOrder)
            .active(true)
            .pinnedAt(LocalDateTime.now())
            .pinnedByUser(pinnedByUser)
            .build();
        
        PinnedProduct saved = pinnedProductRepository.save(pinnedProduct);
        log.info("Successfully pinned product {}", productId);
        
        return saved;
    }
    
    /**
     * Unpin a product
     * 
     * @param pinnedProductId ID of pinned product record
     */
    @Transactional
    public void unpinProduct(Long pinnedProductId) {
        log.info("Unpinning product record {}", pinnedProductId);
        
        PinnedProduct pinnedProduct = pinnedProductRepository.findById(pinnedProductId)
            .orElseThrow(() -> new IllegalArgumentException("Bản ghi ghim không tồn tại"));
        
        pinnedProduct.setActive(false);
        pinnedProductRepository.save(pinnedProduct);
        
        log.info("Successfully unpinned product record {}", pinnedProductId);
    }
    
    /**
     * Reorder pinned products
     * 
     * @param orderList List of pinned product IDs with new display orders
     */
    @Transactional
    public void reorderPinnedProducts(List<PinnedProductOrderDTO> orderList) {
        log.info("Reordering {} pinned products", orderList.size());
        
        for (PinnedProductOrderDTO orderDTO : orderList) {
            PinnedProduct pinnedProduct = pinnedProductRepository.findById(orderDTO.getPinnedProductId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Bản ghi ghim không tồn tại: " + orderDTO.getPinnedProductId()));
            
            pinnedProduct.setDisplayOrder(orderDTO.getDisplayOrder());
            pinnedProductRepository.save(pinnedProduct);
        }
        
        log.info("Successfully reordered pinned products");
    }
    
    /**
     * Get all active pinned products
     * 
     * @return List of pinned products ordered by display order
     */
    @Transactional(readOnly = true)
    public List<PinnedProduct> getPinnedProducts() {
        return pinnedProductRepository.findAllByOrderByDisplayOrderAsc();
    }
}
