package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.service.recommendation.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller xử lý recommendation endpoints
 * Sử dụng @Controller với @ResponseBody cho các AJAX endpoints
 * 
 * Yêu cầu: 5.2, 5.3
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/products")
@Slf4j
public class RecommendationController {
    
    private final RecommendationService recommendationService;
    
    /**
     * Lấy danh sách sản phẩm được gợi ý cho một sản phẩm
     * Endpoint: GET /api/products/{productId}/recommendations
     * 
     * Yêu cầu: 5.2, 5.3
     * 
     * @param productId ID của sản phẩm cần lấy recommendations
     * @return JSON response với danh sách sản phẩm được gợi ý (4-6 sản phẩm)
     */
    @GetMapping("/{productId}/recommendations")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRecommendations(@PathVariable Long productId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.debug("Fetching recommendations for product ID: {}", productId);
            
            // Lấy recommendations từ service (có caching)
            List<Product> recommendations = recommendationService.getRecommendations(productId);
            
            // Build response
            response.put("success", true);
            response.put("productId", productId);
            response.put("recommendations", recommendations);
            response.put("count", recommendations.size());
            response.put("hasRecommendations", !recommendations.isEmpty());
            
            log.debug("Returning {} recommendations for product ID: {}", recommendations.size(), productId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching recommendations for product ID {}: {}", productId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Không thể tải danh sách gợi ý");
            response.put("recommendations", List.of());
            response.put("count", 0);
            response.put("hasRecommendations", false);
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Rebuild recommendations cho một sản phẩm cụ thể
     * Endpoint: POST /api/products/{productId}/recommendations/rebuild
     * 
     * Yêu cầu: 5.2, 5.3
     * 
     * @param productId ID của sản phẩm cần rebuild recommendations
     * @return JSON response xác nhận rebuild thành công
     */
    @PostMapping("/{productId}/recommendations/rebuild")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rebuildRecommendationsForProduct(@PathVariable Long productId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Rebuild recommendations triggered for product ID: {}", productId);
            
            // Rebuild recommendations
            recommendationService.buildRecommendationsForProduct(productId);
            
            // Get updated recommendations count
            long count = recommendationService.countRecommendations(productId);
            
            response.put("success", true);
            response.put("message", "Đã rebuild recommendations thành công");
            response.put("productId", productId);
            response.put("recommendationsCount", count);
            
            log.info("Successfully rebuilt recommendations for product ID: {} (count: {})", productId, count);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Product not found: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
            
        } catch (Exception e) {
            log.error("Error rebuilding recommendations for product ID {}: {}", productId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Không thể rebuild recommendations. Vui lòng thử lại.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Rebuild recommendations cho tất cả sản phẩm
     * Endpoint: POST /api/products/recommendations/rebuild-all
     * 
     * @return JSON response với thống kê rebuild
     */
    @PostMapping("/recommendations/rebuild-all")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rebuildAllRecommendations() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Rebuild all recommendations triggered");
            
            // Rebuild tất cả recommendations
            Map<String, Object> rebuildStats = recommendationService.rebuildAllRecommendationsManually();
            
            response.put("success", true);
            response.put("message", "Đã rebuild tất cả recommendations thành công");
            response.put("stats", rebuildStats);
            
            log.info("Successfully rebuilt all recommendations: {}", rebuildStats);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error rebuilding all recommendations: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Không thể rebuild recommendations. Vui lòng thử lại.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Kiểm tra xem sản phẩm có recommendations không
     * Endpoint: GET /api/products/{productId}/recommendations/check
     * 
     * @param productId ID của sản phẩm
     * @return JSON response với trạng thái có recommendations hay không
     */
    @GetMapping("/{productId}/recommendations/check")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkRecommendations(@PathVariable Long productId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean hasRecommendations = recommendationService.hasRecommendations(productId);
            long count = recommendationService.countRecommendations(productId);
            
            response.put("success", true);
            response.put("productId", productId);
            response.put("hasRecommendations", hasRecommendations);
            response.put("count", count);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error checking recommendations for product ID {}: {}", productId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Không thể kiểm tra recommendations");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Lấy thống kê tổng quan về recommendation system
     * Endpoint: GET /api/products/recommendations/stats
     * 
     * @return JSON response với thống kê hệ thống
     */
    @GetMapping("/recommendations/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRecommendationStats() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.debug("Fetching recommendation system stats");
            
            Map<String, Object> stats = recommendationService.getRecommendationStats();
            
            response.put("success", true);
            response.put("stats", stats);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching recommendation stats: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Không thể lấy thống kê");
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
