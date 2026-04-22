package com.codegym.smartphonemanagement.controller.admin;

import com.codegym.smartphonemanagement.service.dashboard.DTO.OrderAnalyticsDTO;
import com.codegym.smartphonemanagement.service.dashboard.DTO.ProductAnalyticsDTO;
import com.codegym.smartphonemanagement.service.dashboard.seller.OrderAnalyticsService;
import com.codegym.smartphonemanagement.service.dashboard.seller.ProductAnalyticsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Controller
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
@Slf4j
public class AdminAnalyticsController {

    private final OrderAnalyticsService orderAnalyticsService;
    private final ProductAnalyticsService productAnalyticsService;

    @GetMapping("/orders")
    public String showOrderAnalytics(
            @RequestParam(required = false, defaultValue = "7") Integer days,
            @RequestParam(required = false) String customStart,
            @RequestParam(required = false) String customEnd,
            Model model) {

        LocalDateTime start;
        LocalDateTime end = LocalDateTime.now();

        // Xử lý bộ lọc thời gian
        if (customStart != null && !customStart.isEmpty() && customEnd != null && !customEnd.isEmpty()) {
            start = LocalDate.parse(customStart).atStartOfDay();
            end = LocalDate.parse(customEnd).atTime(LocalTime.MAX);
            model.addAttribute("periodDisplay", "Từ " + customStart + " đến " + customEnd);
            model.addAttribute("currentFilter", "custom");
        } else {
            if (days == null || days <= 0) days = 7;
            if (days == 1) {
                start = LocalDate.now().atStartOfDay();
                model.addAttribute("periodDisplay", "Hôm nay");
            } else if (days == 30) {
                start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
                model.addAttribute("periodDisplay", "Tháng này");
            } else {
                start = LocalDateTime.now().minusDays(days);
                model.addAttribute("periodDisplay", days + " ngày qua");
            }
            model.addAttribute("currentFilter", days.toString());
        }

        OrderAnalyticsDTO data = orderAnalyticsService.getAnalytics(start, end);

        model.addAttribute("pageTitle", "order-analytics");
        model.addAttribute("stats", data);
        model.addAttribute("dateFrom", start.toLocalDate().toString());
        model.addAttribute("dateTo", end.toLocalDate().toString());

        return "admin/analytics/orders";
    }

    @GetMapping("/products")
    public String showProductAnalytics(
            @RequestParam(required = false, defaultValue = "30") Integer days,
            @RequestParam(required = false) String customStart,
            @RequestParam(required = false) String customEnd,
            Model model) {

        LocalDateTime start;
        LocalDateTime end = LocalDateTime.now();

        if (customStart != null && !customStart.isEmpty() && customEnd != null && !customEnd.isEmpty()) {
            start = LocalDate.parse(customStart).atStartOfDay();
            end = LocalDate.parse(customEnd).atTime(LocalTime.MAX);
            model.addAttribute("periodDisplay", "Từ " + customStart + " đến " + customEnd);
            model.addAttribute("currentFilter", "custom");
        } else {
            if (days == 1) {
                start = LocalDate.now().atStartOfDay();
                model.addAttribute("periodDisplay", "Hôm nay");
            } else if (days == 30) {
                start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
                model.addAttribute("periodDisplay", "Tháng này");
            } else {
                start = LocalDateTime.now().minusDays(days);
                model.addAttribute("periodDisplay", days + " ngày qua");
            }
            model.addAttribute("currentFilter", days.toString());
        }

        ProductAnalyticsDTO data = productAnalyticsService.getAnalytics(start, end);

        model.addAttribute("pageTitle", "product-analytics");
        model.addAttribute("stats", data);
        model.addAttribute("dateFrom", start.toLocalDate().toString());
        model.addAttribute("dateTo", end.toLocalDate().toString());

        return "admin/analytics/products";
    }

    // Xuất Excel - Có thể thêm vào sau
    @GetMapping("/orders/export")
    public String exportOrderAnalytics() {
        // Implement export logic later if needed
        return "redirect:/admin/analytics/orders";
    }

    /**
     * Export Product Analytics to CSV
     */
    @GetMapping("/products/export/csv")
    public void exportProductAnalyticsCsv(
            @RequestParam(required = false, defaultValue = "30") Integer days,
            @RequestParam(required = false) String customStart,
            @RequestParam(required = false) String customEnd,
            HttpServletResponse response) throws IOException {
        
        log.info("Exporting product analytics to CSV - days: {}, customStart: {}, customEnd: {}", 
                 days, customStart, customEnd);
        
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=product_analytics_export_" + LocalDate.now() + ".csv");
        
        // Calculate date range (same logic as showProductAnalytics)
        LocalDateTime start;
        LocalDateTime end = LocalDateTime.now();
        
        if (customStart != null && !customStart.isEmpty() && customEnd != null && !customEnd.isEmpty()) {
            start = LocalDate.parse(customStart).atStartOfDay();
            end = LocalDate.parse(customEnd).atTime(LocalTime.MAX);
        } else {
            if (days == 1) {
                start = LocalDate.now().atStartOfDay();
            } else if (days == 30) {
                start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            } else {
                start = LocalDateTime.now().minusDays(days);
            }
        }
        
        // Get analytics data
        ProductAnalyticsDTO data = productAnalyticsService.getAnalytics(start, end);
        
        PrintWriter writer = response.getWriter();
        // BOM for Excel UTF-8
        writer.write('\uFEFF');
        writer.println("ID Sản Phẩm,Tên Sản Phẩm,Danh Mục,Giá Trị,Doanh Thu,Tỷ Lệ (%),Tồn Kho,Trạng Thái");
        
        // Export Top Selling Products
        if (data.getTopSellingProducts() != null) {
            for (ProductAnalyticsDTO.ProductMetricDTO product : data.getTopSellingProducts()) {
                writeProductRow(writer, product, "Bán Chạy");
            }
        }
        
        // Export Top Revenue Products
        if (data.getTopRevenueProducts() != null) {
            for (ProductAnalyticsDTO.ProductMetricDTO product : data.getTopRevenueProducts()) {
                writeProductRow(writer, product, "Doanh Thu Cao");
            }
        }
        
        // Export Low Stock Products
        if (data.getLowStockProducts() != null) {
            for (ProductAnalyticsDTO.ProductMetricDTO product : data.getLowStockProducts()) {
                writeProductRow(writer, product, "Tồn Kho Thấp");
            }
        }
        
        // Export Most Wishlisted Products
        if (data.getMostWishlistedProducts() != null) {
            for (ProductAnalyticsDTO.ProductMetricDTO product : data.getMostWishlistedProducts()) {
                writeProductRow(writer, product, "Yêu Thích Nhiều");
            }
        }
        
        // Export Most Viewed Products
        if (data.getMostViewedProducts() != null) {
            for (ProductAnalyticsDTO.ProductMetricDTO product : data.getMostViewedProducts()) {
                writeProductRow(writer, product, "Xem Nhiều");
            }
        }
        
        // Export Top Conversion Products
        if (data.getTopConversionProducts() != null) {
            for (ProductAnalyticsDTO.ProductMetricDTO product : data.getTopConversionProducts()) {
                writeProductRow(writer, product, "Chuyển Đổi Cao");
            }
        }
        
        writer.flush();
    }
    
    /**
     * Helper method to write a product row to CSV
     */
    private void writeProductRow(PrintWriter writer, ProductAnalyticsDTO.ProductMetricDTO product, String category) {
        String line = String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                product.getId() != null ? product.getId().toString() : "N/A",
                escape(product.getName()),
                escape(product.getCategory()),
                product.getValue() != null ? product.getValue().toString() : "0",
                product.getRevenue() != null ? product.getRevenue().toPlainString() : "0",
                product.getPercentage() != null ? String.format("%.2f", product.getPercentage()) : "0",
                product.getStock() != null ? product.getStock().toString() : "0",
                category);
        writer.println(line);
    }
    
    /**
     * Escape CSV values
     */
    private String escape(String val) {
        if (val == null) return "";
        return val.replace("\"", "\"\"");
    }
}
