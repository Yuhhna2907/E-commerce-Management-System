package com.codegym.smartphonemanagement.controller.admin;

import com.codegym.smartphonemanagement.service.dashboard.seller.SearchAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequestMapping("/admin/analytics/search")
@RequiredArgsConstructor
public class AdminSearchAnalyticsController {

    private final SearchAnalyticsService searchAnalyticsService;

    @GetMapping
    public String searchAnalyticsDashboard(
            @RequestParam(defaultValue = "7") int days,
            Model model) {
        
        Map<String, Object> analytics = searchAnalyticsService.getSearchAnalyticsSummary(days);
        
        model.addAttribute("topTrends", analytics.get("topTrends"));
        model.addAttribute("totalSearches", analytics.get("totalSearches"));
        model.addAttribute("totalConversions", analytics.get("totalConversions"));
        model.addAttribute("avgConversionRate", analytics.get("avgConversionRate"));
        model.addAttribute("periodDays", days);
        model.addAttribute("activePage", "analytics-search");
        
        return "admin/analytics/search";
    }
}
