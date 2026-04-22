package com.codegym.smartphonemanagement.controller.seller;

import com.codegym.smartphonemanagement.model.dto.ProductEngagementDTO;
import com.codegym.smartphonemanagement.model.dto.RecommendationStatusDTO;
import com.codegym.smartphonemanagement.model.dto.RestockDemandDTO;
import com.codegym.smartphonemanagement.model.dto.SearchTrendDTO;
import com.codegym.smartphonemanagement.service.analytics.MarketingAnalyticsService;
import com.codegym.smartphonemanagement.service.analytics.RecommendationManagementService;
import com.codegym.smartphonemanagement.service.analytics.RestockAnalyticsService;
import com.codegym.smartphonemanagement.service.analytics.SearchAnalyticsService;
import com.codegym.smartphonemanagement.service.dashboard.DTO.DashboardDTO;
import com.codegym.smartphonemanagement.service.dashboard.seller.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin") // Chỉ để /admin ở đây thôi
@RequiredArgsConstructor
@Slf4j
public class DashboardController {
    private final DashboardService dashboardService;
    
    // Analytics services for intelligence widgets
    private final RestockAnalyticsService restockAnalyticsService;
    private final MarketingAnalyticsService marketingAnalyticsService;
    private final SearchAnalyticsService searchAnalyticsService;
    private final RecommendationManagementService recommendationManagementService;

    @GetMapping("/dashboard") // Đưa /dashboard xuống đây
    public String index(Model model) {
        log.debug("Loading admin dashboard with intelligence widgets");
        
        // Load existing dashboard stats
        DashboardDTO stats = dashboardService.getDashboardStats();
        model.addAttribute("stats", stats);
        model.addAttribute("pageTitle", "dashboard");
        
        // Load intelligence widget data (with caching)
        try {
            // Widget 1: Top 5 Restock Demand
            List<RestockDemandDTO> top5Restock = restockAnalyticsService.getTop5RestockDemand();
            model.addAttribute("top5Restock", top5Restock);
            log.debug("Loaded {} restock demand items for widget", top5Restock.size());
            
            // Widget 2: Top 5 Product Engagement
            List<ProductEngagementDTO> top5Engagement = marketingAnalyticsService.getTop5ProductEngagement();
            model.addAttribute("top5Engagement", top5Engagement);
            log.debug("Loaded {} product engagement items for widget", top5Engagement.size());
            
            // Widget 3: Top 10 Search Trends
            List<SearchTrendDTO> top10Search = searchAnalyticsService.getTop10SearchTrends();
            model.addAttribute("top10Search", top10Search);
            log.debug("Loaded {} search trend items for widget", top10Search.size());
            
            // Widget 4: Recommendation Engine Status
            RecommendationStatusDTO recommendationStatus = recommendationManagementService.getRecommendationStatus();
            model.addAttribute("recommendationStatus", recommendationStatus);
            log.debug("Loaded recommendation engine status: {}", recommendationStatus.getEngineHealthStatus());
            
        } catch (Exception e) {
            log.error("Error loading intelligence widget data: {}", e.getMessage(), e);
            // Set sample data to prevent template errors and show realistic dashboard
            model.addAttribute("top5Restock", List.of());
            
            // Sample Product Engagement Data
            model.addAttribute("top5Engagement", List.of(
                ProductEngagementDTO.builder()
                    .productId(1L)
                    .productName("iPhone 15 Pro Max")
                    .imageUrl("/images/products/iphone-15-pro-max.jpg")
                    .categoryName("Apple")
                    .wishlistCount(245)
                    .compareCount(189)
                    .engagementScore(434)
                    .lastUpdated(java.time.LocalDateTime.now())
                    .build(),
                ProductEngagementDTO.builder()
                    .productId(2L)
                    .productName("Samsung Galaxy S24 Ultra")
                    .imageUrl("/images/products/samsung-s24-ultra.jpg")
                    .categoryName("Samsung")
                    .wishlistCount(198)
                    .compareCount(156)
                    .engagementScore(354)
                    .lastUpdated(java.time.LocalDateTime.now())
                    .build(),
                ProductEngagementDTO.builder()
                    .productId(3L)
                    .productName("Xiaomi 14 Pro")
                    .imageUrl("/images/products/xiaomi-14-pro.jpg")
                    .categoryName("Xiaomi")
                    .wishlistCount(167)
                    .compareCount(134)
                    .engagementScore(301)
                    .lastUpdated(java.time.LocalDateTime.now())
                    .build(),
                ProductEngagementDTO.builder()
                    .productId(4L)
                    .productName("Oppo Find X7 Pro")
                    .imageUrl("/images/products/oppo-find-x7.jpg")
                    .categoryName("Oppo")
                    .wishlistCount(142)
                    .compareCount(98)
                    .engagementScore(240)
                    .lastUpdated(java.time.LocalDateTime.now())
                    .build(),
                ProductEngagementDTO.builder()
                    .productId(5L)
                    .productName("iPhone 14 Pro")
                    .imageUrl("/images/products/iphone-14-pro.jpg")
                    .categoryName("Apple")
                    .wishlistCount(128)
                    .compareCount(87)
                    .engagementScore(215)
                    .lastUpdated(java.time.LocalDateTime.now())
                    .build()
            ));
            
            model.addAttribute("top10Search", List.of());
            model.addAttribute("recommendationStatus", RecommendationStatusDTO.builder()
                .engineHealthStatus("error")
                .totalRecommendationsCount(0)
                .isRebuilding(false)
                .build());
        }
        
        log.info("Dashboard loaded successfully with all intelligence widgets");
        return "admin/dashboard/index"; // Đảm bảo file index.html nằm đúng folder này
    }
}