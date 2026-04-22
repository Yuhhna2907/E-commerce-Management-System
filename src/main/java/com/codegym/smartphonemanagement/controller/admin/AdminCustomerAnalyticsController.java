package com.codegym.smartphonemanagement.controller.admin;

import com.codegym.smartphonemanagement.service.dashboard.DTO.CustomerAnalyticsDTO;
import com.codegym.smartphonemanagement.service.dashboard.seller.CustomerAnalyticsService;
import com.codegym.smartphonemanagement.service.dashboard.seller.CustomerServiceAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
public class AdminCustomerAnalyticsController {

    private final CustomerAnalyticsService customerAnalyticsService;
    private final CustomerServiceAnalyticsService customerServiceAnalyticsService;

    @GetMapping("/customers")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER')")
    public String viewCustomerAnalytics(Model model) {
        CustomerAnalyticsDTO stats = customerAnalyticsService.getCustomerAnalytics();
        model.addAttribute("stats", stats);
        model.addAttribute("activePage", "analytics-customers");
        return "admin/analytics/customers";
    }

    @GetMapping("/customers/export-csv")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER')")
    public org.springframework.http.ResponseEntity<String> exportCustomerDataCSV() {
        CustomerAnalyticsDTO stats = customerAnalyticsService.getCustomerAnalytics();
        
        StringBuilder csvBuilder = new StringBuilder();
        // BOM for Excel UTF-8
        csvBuilder.append('\uFEFF');
        
        csvBuilder.append("REPORT: CHI TIẾT TỔNG QUAN KHÁCH HÀNG\n");
        csvBuilder.append("Tổng Users,Users Active,Đăng ký mới trong tháng,Tài khoản bị khóa\n");
        csvBuilder.append(String.format("%d,%d,%d,%d\n\n", 
                stats.getTotalUsers(), stats.getActiveUsers(), 
                stats.getNewUsersThisMonth(), stats.getBannedUsers()));
                
        csvBuilder.append("REPORT: DANH SÁCH TOP ĐẠI GIA (VIPs)\n");
        csvBuilder.append("Username,Hạng thành viên,Tổng điểm tích lũy\n");
        if (stats.getTopSpendersVIPs() != null) {
            for (CustomerAnalyticsDTO.UserMetric u : stats.getTopSpendersVIPs()) {
                csvBuilder.append(String.format("%s,%s,%s\n", 
                        u.getUsername() != null ? u.getUsername().replace(",", "") : "", 
                        u.getTierOrRole() != null ? u.getTierOrRole() : "", 
                        u.getMetricValue() != null ? u.getMetricValue() : "0"));
            }
        }
        
        csvBuilder.append("\nREPORT: DANH SÁCH RỦI RO (BOOM HÀNG CAO)\n");
        csvBuilder.append("Username,Số đơn hủy/trả\n");
        if (stats.getHighReturnRateUsers() != null) {
            for (CustomerAnalyticsDTO.UserMetric r : stats.getHighReturnRateUsers()) {
                csvBuilder.append(String.format("%s,%s\n", 
                        r.getUsername() != null ? r.getUsername().replace(",", "") : "", 
                        r.getMetricValue() != null ? r.getMetricValue().replace(" Orders", "") : "0"));
            }
        }
        
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"customer_analytics_report.csv\"");
        headers.add(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");
        
        return new org.springframework.http.ResponseEntity<>(csvBuilder.toString(), headers, org.springframework.http.HttpStatus.OK);
    }

    @GetMapping("/customer-service")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER')")
    public String viewCustomerServiceAnalytics(
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "30") Integer days,
            org.springframework.ui.Model model) {
        
        java.time.LocalDateTime end = java.time.LocalDateTime.now();
        java.time.LocalDateTime start = end.minusDays(days);
        
        com.codegym.smartphonemanagement.service.dashboard.DTO.CustomerServiceAnalyticsDTO stats = 
                customerServiceAnalyticsService.getCSAnalytics(start, end);
                
        model.addAttribute("stats", stats);
        model.addAttribute("days", days);
        model.addAttribute("activePage", "analytics-cs");
        return "admin/analytics/customer_service";
    }
}
