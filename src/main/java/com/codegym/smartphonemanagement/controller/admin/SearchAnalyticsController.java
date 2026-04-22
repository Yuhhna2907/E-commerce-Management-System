package com.codegym.smartphonemanagement.controller.admin;

import com.codegym.smartphonemanagement.model.dto.SearchTrendDTO;
import com.codegym.smartphonemanagement.service.analytics.ExportService;
import com.codegym.smartphonemanagement.service.analytics.SearchAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Search Trends Analytics
 * Provides detail page with date range filtering, sorting, pagination, and export
 */
@Controller
@RequestMapping("/admin/analytics/search-trends")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class SearchAnalyticsController {
    
    private final SearchAnalyticsService searchAnalyticsService;
    private final ExportService exportService;
    
    /**
     * Display search trends analytics page
     */
    @GetMapping
    public String getSearchTrendsPage(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "searchCount") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Model model) {
        
        log.info("Accessing search trends page - start: {}, end: {}, sortBy: {}, page: {}", 
                 startDate, endDate, sortBy, page);
        
        // Validate page size
        if (size < 1 || size > 100) {
            size = 25;
        }
        
        // Default to last 7 days if not specified
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(7);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        // Get paginated data
        Pageable pageable = PageRequest.of(page, size);
        Page<SearchTrendDTO> searchData = searchAnalyticsService.getSearchTrendsData(
            startDate, endDate, sortBy, sortDir, pageable);
        
        // Add data to model
        model.addAttribute("searchData", searchData);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        
        // Add breadcrumb
        List<com.codegym.smartphonemanagement.dto.BreadcrumbItem> breadcrumbs = List.of(
            new com.codegym.smartphonemanagement.dto.BreadcrumbItem("Dashboard", "/admin/dashboard", false),
            new com.codegym.smartphonemanagement.dto.BreadcrumbItem("Analytics", null, false),
            new com.codegym.smartphonemanagement.dto.BreadcrumbItem("Xu Hướng Tìm Kiếm", "/admin/analytics/search-trends", true)
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        return "admin/analytics/search-trends";
    }
    
    /**
     * Export search trends data to CSV
     */
    @GetMapping("/export/csv")
    public ResponseEntity<ByteArrayResource> exportCsv(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        log.info("Exporting search trends to CSV - start: {}, end: {}", startDate, endDate);
        
        // Get all data (no pagination)
        List<SearchTrendDTO> data = searchAnalyticsService.getAllSearchTrendsData(startDate, endDate);
        
        // Prepare headers
        List<String> headers = List.of(
            "Từ Khóa Tìm Kiếm",
            "Số Lượt Tìm Kiếm",
            "Số Lượt Chuyển Đổi",
            "Tỷ Lệ Chuyển Đổi (%)",
            "Ngày Tìm Kiếm Đầu Tiên",
            "Ngày Tìm Kiếm Gần Nhất"
        );
        
        // Prepare rows
        List<List<String>> rows = new ArrayList<>();
        for (SearchTrendDTO dto : data) {
            List<String> row = List.of(
                dto.getSearchKeyword(),
                dto.getSearchCount().toString(),
                dto.getConversionCount().toString(),
                dto.getConversionRate() != null ? dto.getConversionRate().toString() : "0.00",
                dto.getFirstSearchedDate() != null ? dto.getFirstSearchedDate().toString() : "N/A",
                dto.getLastSearchedDate() != null ? dto.getLastSearchedDate().toString() : "N/A"
            );
            rows.add(row);
        }
        
        // Generate CSV
        ByteArrayResource resource = exportService.exportToCsv(
            headers, rows, "xu-huong-tim-kiem");
        String filename = exportService.generateFilename("xu-huong-tim-kiem", "csv");
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(resource);
    }
    
    /**
     * Export search trends data to Excel
     */
    @GetMapping("/export/excel")
    public ResponseEntity<ByteArrayResource> exportExcel(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        log.info("Exporting search trends to Excel - start: {}, end: {}", startDate, endDate);
        
        // Get all data (no pagination)
        List<SearchTrendDTO> data = searchAnalyticsService.getAllSearchTrendsData(startDate, endDate);
        
        // Prepare headers
        List<String> headers = List.of(
            "Từ Khóa Tìm Kiếm",
            "Số Lượt Tìm Kiếm",
            "Số Lượt Chuyển Đổi",
            "Tỷ Lệ Chuyển Đổi (%)",
            "Ngày Tìm Kiếm Đầu Tiên",
            "Ngày Tìm Kiếm Gần Nhất"
        );
        
        // Prepare rows
        List<List<String>> rows = new ArrayList<>();
        for (SearchTrendDTO dto : data) {
            List<String> row = List.of(
                dto.getSearchKeyword(),
                dto.getSearchCount().toString(),
                dto.getConversionCount().toString(),
                dto.getConversionRate() != null ? dto.getConversionRate().toString() : "0.00",
                dto.getFirstSearchedDate() != null ? dto.getFirstSearchedDate().toString() : "N/A",
                dto.getLastSearchedDate() != null ? dto.getLastSearchedDate().toString() : "N/A"
            );
            rows.add(row);
        }
        
        // Generate Excel
        ByteArrayResource resource = exportService.exportToExcel(
            headers, rows, "xu-huong-tim-kiem", "Xu Hướng Tìm Kiếm");
        String filename = exportService.generateFilename("xu-huong-tim-kiem", "xlsx");
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(resource);
    }
}
