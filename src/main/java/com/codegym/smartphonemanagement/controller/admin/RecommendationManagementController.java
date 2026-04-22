package com.codegym.smartphonemanagement.controller.admin;

import com.codegym.smartphonemanagement.model.PinnedProduct;
import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.model.dto.PinnedProductOrderDTO;
import com.codegym.smartphonemanagement.model.dto.RebuildResultDTO;
import com.codegym.smartphonemanagement.model.dto.RecommendationStatusDTO;
import com.codegym.smartphonemanagement.service.analytics.RecommendationManagementService;
import com.codegym.smartphonemanagement.util.AnalyticsAuditHelper;
import com.codegym.smartphonemanagement.util.SecurityAuditLogger;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for Recommendation Management
 * Provides admin controls for recommendation engine and pinned products
 */
@Controller
@RequestMapping("/admin/analytics/recommendations")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class RecommendationManagementController {
    
    private final RecommendationManagementService recommendationManagementService;
    private final SecurityAuditLogger securityAuditLogger;
    private final AnalyticsAuditHelper auditHelper;
    
    /**
     * Display recommendations management page
     */
    @GetMapping
    public String getRecommendationsPage(Model model) {
        log.info("Accessing recommendations management page");
        
        // Get recommendation status
        RecommendationStatusDTO status = recommendationManagementService.getRecommendationStatus();
        model.addAttribute("recommendationStatus", status);
        
        // Get pinned products
        List<PinnedProduct> pinnedProducts = recommendationManagementService.getPinnedProducts();
        model.addAttribute("pinnedProducts", pinnedProducts);
        
        // Add breadcrumb
        List<com.codegym.smartphonemanagement.dto.BreadcrumbItem> breadcrumbs = List.of(
            new com.codegym.smartphonemanagement.dto.BreadcrumbItem("Dashboard", "/admin/dashboard", false),
            new com.codegym.smartphonemanagement.dto.BreadcrumbItem("Analytics", null, false),
            new com.codegym.smartphonemanagement.dto.BreadcrumbItem("Quản Lý Gợi Ý", "/admin/analytics/recommendations", true)
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        return "admin/analytics/recommendations";
    }
    
    /**
     * Trigger recommendation matrix rebuild
     * Returns immediately with status, actual rebuild happens asynchronously
     * Requirements: 14.3, 14.4
     */
    @PostMapping("/rebuild")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rebuildRecommendations(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {
        
        String username = userDetails.getUsername();
        String ipAddress = getClientIpAddress(request);
        
        log.info("Rebuild recommendations triggered by admin: {}", username);
        
        try {
            // Trigger async rebuild
            CompletableFuture<RebuildResultDTO> future = 
                recommendationManagementService.rebuildRecommendationMatrix();
            
            // Audit log rebuild action
            securityAuditLogger.logRecommendationRebuild(
                username, ipAddress, true, "Rebuild started successfully");
            
            // Return immediate response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã bắt đầu rebuild recommendation matrix");
            response.put("status", "in_progress");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to trigger rebuild", e);
            
            // Audit log rebuild failure
            securityAuditLogger.logRecommendationRebuild(
                username, ipAddress, false, "Failed to start rebuild: " + e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Không thể bắt đầu rebuild: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Get rebuild status (for polling)
     */
    @GetMapping("/rebuild/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRebuildStatus() {
        Map<String, Object> status = recommendationManagementService.getRebuildProgress();
        return ResponseEntity.ok(status);
    }
    
    /**
     * Pin a product to homepage
     * Requirements: 14.3, 14.4
     */
    @PostMapping("/pin")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> pinProduct(
            @RequestParam Long productId,
            @RequestParam int displayOrder,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {
        
        String username = userDetails.getUsername();
        String ipAddress = getClientIpAddress(request);
        
        log.info("Pinning product {} with order {} by user {}", 
                 productId, displayOrder, username);
        
        try {
            // Get current user (simplified - in real app, fetch from UserRepository)
            User currentUser = new User();
            currentUser.setUsername(username);
            
            PinnedProduct pinnedProduct = recommendationManagementService.pinProduct(
                productId, displayOrder, currentUser);
            
            // Audit log pin action
            securityAuditLogger.logPinnedProductAction(username, "PIN", productId, ipAddress);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã ghim sản phẩm thành công");
            response.put("pinnedProductId", pinnedProduct.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Failed to pin product: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("Error pinning product", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Unpin a product
     * Requirements: 14.3, 14.4
     */
    @DeleteMapping("/pin/{pinnedProductId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> unpinProduct(
            @PathVariable Long pinnedProductId,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {
        
        String username = userDetails.getUsername();
        String ipAddress = getClientIpAddress(request);
        
        log.info("Unpinning product record {} by user {}", pinnedProductId, username);
        
        try {
            recommendationManagementService.unpinProduct(pinnedProductId);
            
            // Audit log unpin action
            securityAuditLogger.logPinnedProductAction(username, "UNPIN", pinnedProductId, ipAddress);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã bỏ ghim sản phẩm thành công");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Failed to unpin product: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("Error unpinning product", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Reorder pinned products
     * Requirements: 14.3, 14.4
     */
    @PutMapping("/pin/reorder")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reorderPinnedProducts(
            @RequestBody List<PinnedProductOrderDTO> orderList,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {
        
        String username = userDetails.getUsername();
        String ipAddress = getClientIpAddress(request);
        
        log.info("Reordering {} pinned products by user {}", orderList.size(), username);
        
        try {
            recommendationManagementService.reorderPinnedProducts(orderList);
            
            // Audit log reorder action
            securityAuditLogger.logPinnedProductAction(
                username, "REORDER", null, ipAddress);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã cập nhật thứ tự sản phẩm ghim");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error reordering pinned products", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        return auditHelper.getClientIpAddress(request);
    }
}
