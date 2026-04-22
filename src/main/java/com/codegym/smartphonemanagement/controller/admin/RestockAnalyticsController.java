package com.codegym.smartphonemanagement.controller.admin;

import com.codegym.smartphonemanagement.model.dto.RestockDemandDTO;
import com.codegym.smartphonemanagement.service.analytics.ExportService;
import com.codegym.smartphonemanagement.service.analytics.RestockAnalyticsService;
import com.codegym.smartphonemanagement.service.category.CategoryService;
import com.codegym.smartphonemanagement.util.AnalyticsAuditHelper;
import jakarta.servlet.http.HttpServletRequest;
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
 * Controller for Restock Demand Analytics
 * Provides detail page with filtering, sorting, pagination, and export
 */
@Controller
@RequestMapping("/admin/analytics/restock-demand")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class RestockAnalyticsController {
    
    private final RestockAnalyticsService restockAnalyticsService;
    private final ExportService exportService;
    private final CategoryService categoryService;
    private final AnalyticsAuditHelper auditHelper;
    
    /**
     * Display restock demand analytics page
     */
    @GetMapping
    public String getRestockDemandPage(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "notificationCount") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Model model) {
        
        log.info("Accessing restock demand page - categoryId: {}, sortBy: {}, page: {}", 
                 categoryId, sortBy, page);
        
        // Validate page size
        if (size < 1 || size > 100) {
            size = 25;
        }
        
        // Get paginated data
        Pageable pageable = PageRequest.of(page, size);
        Page<RestockDemandDTO> restockData = restockAnalyticsService.getRestockDemandData(
            categoryId, sortBy, sortDir, pageable);
        
        // Add data to model
        model.addAttribute("restockData", restockData);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        
        // Add breadcrumb
        List<com.codegym.smartphonemanagement.dto.BreadcrumbItem> breadcrumbs = List.of(
            new com.codegym.smartphonemanagement.dto.BreadcrumbItem("Dashboard", "/admin/dashboard", false),
            new com.codegym.smartphonemanagement.dto.BreadcrumbItem("Analytics", null, false),
            new com.codegym.smartphonemanagement.dto.BreadcrumbItem("Nhu Cầu Nhập Hàng", "/admin/analytics/restock-demand", true)
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        return "admin/analytics/restock-demand";
    }
    
    /**
     * Export restock demand data to CSV
     * Requirements: 14.3, 14.4
     */
    @GetMapping("/export/csv")
    public ResponseEntity<ByteArrayResource> exportCsv(
            @RequestParam(required = false) Long categoryId,
            HttpServletRequest request) {
        
        log.info("Exporting restock demand to CSV - categoryId: {}", categoryId);
        
        // Audit log export operation
        String filters = auditHelper.buildFilterString("categoryId", categoryId);
        auditHelper.logExport(request, "CSV", "restock-demand", filters);
        
        // Get all data (no pagination)
        List<RestockDemandDTO> data = restockAnalyticsService.getAllRestockDemandData(categoryId);
        
        // Prepare headers
        List<String> headers = List.of(
            "ID Sản Phẩm",
            "Tên Sản Phẩm",
            "Danh Mục",
            "Trạng Thái Kho",
            "Số Lượng Đăng Ký",
            "Ngày Hết Hàng Gần Nhất"
        );
        
        // Prepare rows
        List<List<String>> rows = new ArrayList<>();
        for (RestockDemandDTO dto : data) {
            List<String> row = List.of(
                dto.getProductId().toString(),
                dto.getProductName(),
                dto.getCategoryName(),
                dto.getStockStatus(),
                dto.getNotificationCount().toString(),
                dto.getLastOutOfStockDate() != null ? 
                    dto.getLastOutOfStockDate().toString() : "N/A"
            );
            rows.add(row);
        }
        
        // Generate CSV
        ByteArrayResource resource = exportService.exportToCsv(headers, rows, "nhu-cau-nhap-hang");
        String filename = exportService.generateFilename("nhu-cau-nhap-hang", "csv");
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(resource);
    }
    
    /**
     * Export restock demand data to Excel
     * Requirements: 14.3, 14.4
     */
    @GetMapping("/export/excel")
    public ResponseEntity<ByteArrayResource> exportExcel(
            @RequestParam(required = false) Long categoryId,
            HttpServletRequest request) {
        
        log.info("Exporting restock demand to Excel - categoryId: {}", categoryId);
        
        // Audit log export operation
        String filters = auditHelper.buildFilterString("categoryId", categoryId);
        auditHelper.logExport(request, "Excel", "restock-demand", filters);
        
        // Get all data (no pagination)
        List<RestockDemandDTO> data = restockAnalyticsService.getAllRestockDemandData(categoryId);
        
        // Prepare headers
        List<String> headers = List.of(
            "ID Sản Phẩm",
            "Tên Sản Phẩm",
            "Danh Mục",
            "Trạng Thái Kho",
            "Số Lượng Đăng Ký",
            "Ngày Hết Hàng Gần Nhất"
        );
        
        // Prepare rows
        List<List<String>> rows = new ArrayList<>();
        for (RestockDemandDTO dto : data) {
            List<String> row = List.of(
                dto.getProductId().toString(),
                dto.getProductName(),
                dto.getCategoryName(),
                dto.getStockStatus(),
                dto.getNotificationCount().toString(),
                dto.getLastOutOfStockDate() != null ? 
                    dto.getLastOutOfStockDate().toString() : "N/A"
            );
            rows.add(row);
        }
        
        // Generate Excel
        ByteArrayResource resource = exportService.exportToExcel(
            headers, rows, "nhu-cau-nhap-hang", "Nhu Cầu Nhập Hàng");
        String filename = exportService.generateFilename("nhu-cau-nhap-hang", "xlsx");
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(resource);
    }
}
