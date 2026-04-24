package com.codegym.smartphonemanagement.controller.admin;
import com.codegym.smartphonemanagement.model.dto.ProductEngagementDTO;
import com.codegym.smartphonemanagement.service.analytics.ExportService;
import com.codegym.smartphonemanagement.service.analytics.MarketingAnalyticsService;
import com.codegym.smartphonemanagement.service.category.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Product Engagement Analytics & Product Management Statistics
 * Provides detail page with filtering, sorting, pagination, and export
 */
@Controller
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class MarketingAnalyticsController {
    
    private final MarketingAnalyticsService marketingAnalyticsService;
    private final ExportService exportService;
    private final CategoryService categoryService;
    
    /**
     * Display product management statistics page (Trang Thống Kê Sản Phẩm)
     */
    @GetMapping("/admin/product-management")
    public String getProductManagementPage(Model model) {
        log.info("Accessing product management statistics page");
        
        // Add breadcrumb
        List<com.codegym.smartphonemanagement.dto.BreadcrumbItem> breadcrumbs = List.of(
            new com.codegym.smartphonemanagement.dto.BreadcrumbItem("Dashboard", "/admin/dashboard", false),
            new com.codegym.smartphonemanagement.dto.BreadcrumbItem("Thống Kê Sản Phẩm", "/admin/product-management", true)
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        model.addAttribute("pageTitle", "product-management");
        
        return "admin/product/management";
    }
    
    /**
     * Display product engagement analytics page
     */
    @GetMapping("/admin/analytics/product-engagement")
    public String getProductEngagementPage(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer minEngagementScore,
            @RequestParam(defaultValue = "engagementScore") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Model model) {
        
        log.info("Accessing product engagement page - categoryId: {}, minScore: {}, sortBy: {}, page: {}", 
                 categoryId, minEngagementScore, sortBy, page);
        
        // Validate page size
        if (size < 1 || size > 100) {
            size = 25;
        }
        
        // Get paginated data
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductEngagementDTO> engagementData = marketingAnalyticsService.getProductEngagementData(
            categoryId, minEngagementScore, sortBy, sortDir, pageable);
        
        // Add data to model
        model.addAttribute("engagementData", engagementData);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("minEngagementScore", minEngagementScore);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        
        // Add breadcrumb
        List<com.codegym.smartphonemanagement.dto.BreadcrumbItem> breadcrumbs = List.of(
            new com.codegym.smartphonemanagement.dto.BreadcrumbItem("Dashboard", "/admin/dashboard", false),
            new com.codegym.smartphonemanagement.dto.BreadcrumbItem("Analytics", null, false),
            new com.codegym.smartphonemanagement.dto.BreadcrumbItem("Mức Độ Quan Tâm Sản Phẩm", "/admin/analytics/product-engagement", true)
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        return "admin/analytics/product-engagement";
    }
    
    /**
     * Export product engagement data to CSV
     */
    @GetMapping("/export/csv")
    public ResponseEntity<ByteArrayResource> exportCsv(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer minEngagementScore) {
        
        log.info("Exporting product engagement to CSV - categoryId: {}, minScore: {}", 
                 categoryId, minEngagementScore);
        
        // Get all data (no pagination)
        List<ProductEngagementDTO> data = marketingAnalyticsService.getAllProductEngagementData(
            categoryId, minEngagementScore);
        
        // Prepare headers
        List<String> headers = List.of(
            "ID Sản Phẩm",
            "Tên Sản Phẩm",
            "Danh Mục",
            "Số Lượt Wishlist",
            "Số Lượt So Sánh",
            "Điểm Quan Tâm",
            "Cập Nhật Lần Cuối"
        );
        
        // Prepare rows
        List<List<String>> rows = new ArrayList<>();
        for (ProductEngagementDTO dto : data) {
            List<String> row = List.of(
                dto.getProductId().toString(),
                dto.getProductName(),
                dto.getCategoryName(),
                dto.getWishlistCount().toString(),
                dto.getCompareCount().toString(),
                dto.getEngagementScore().toString(),
                dto.getLastUpdated() != null ? dto.getLastUpdated().toString() : "N/A"
            );
            rows.add(row);
        }
        
        // Generate CSV
        ByteArrayResource resource = exportService.exportToCsv(
            headers, rows, "muc-do-quan-tam-san-pham");
        String filename = exportService.generateFilename("muc-do-quan-tam-san-pham", "csv");
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(resource);
    }
    
    /**
     * Export product engagement data to Excel
     */
    @GetMapping("/export/excel")
    public ResponseEntity<ByteArrayResource> exportExcel(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer minEngagementScore) {
        
        log.info("Exporting product engagement to Excel - categoryId: {}, minScore: {}", 
                 categoryId, minEngagementScore);
        
        // Get all data (no pagination)
        List<ProductEngagementDTO> data = marketingAnalyticsService.getAllProductEngagementData(
            categoryId, minEngagementScore);
        
        // Prepare headers
        List<String> headers = List.of(
            "ID Sản Phẩm",
            "Tên Sản Phẩm",
            "Danh Mục",
            "Số Lượt Wishlist",
            "Số Lượt So Sánh",
            "Điểm Quan Tâm",
            "Cập Nhật Lần Cuối"
        );
        
        // Prepare rows
        List<List<String>> rows = new ArrayList<>();
        for (ProductEngagementDTO dto : data) {
            List<String> row = List.of(
                dto.getProductId().toString(),
                dto.getProductName(),
                dto.getCategoryName(),
                dto.getWishlistCount().toString(),
                dto.getCompareCount().toString(),
                dto.getEngagementScore().toString(),
                dto.getLastUpdated() != null ? dto.getLastUpdated().toString() : "N/A"
            );
            rows.add(row);
        }
        
        // Generate Excel
        ByteArrayResource resource = exportService.exportToExcel(
            headers, rows, "muc-do-quan-tam-san-pham", "Mức Độ Quan Tâm");
        String filename = exportService.generateFilename("muc-do-quan-tam-san-pham", "xlsx");
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(resource);
    }
}
