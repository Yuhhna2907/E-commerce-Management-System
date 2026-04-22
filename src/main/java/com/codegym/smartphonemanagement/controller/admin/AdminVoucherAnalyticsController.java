package com.codegym.smartphonemanagement.controller.admin;

import com.codegym.smartphonemanagement.service.dashboard.DTO.VoucherAnalyticsDTO;
import com.codegym.smartphonemanagement.service.dashboard.seller.VoucherAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
public class AdminVoucherAnalyticsController {

    private final VoucherAnalyticsService voucherAnalyticsService;

    @GetMapping("/marketing")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER')")
    public String viewMarketingAnalytics(Model model) {
        VoucherAnalyticsDTO stats = voucherAnalyticsService.getVoucherAnalytics();
        model.addAttribute("stats", stats);
        model.addAttribute("activePage", "analytics-marketing");
        return "admin/analytics/vouchers";
    }

    @GetMapping("/marketing/marketing-export-csv")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER')")
    public ResponseEntity<byte[]> exportMarketingDataCSV() {
        VoucherAnalyticsDTO stats = voucherAnalyticsService.getVoucherAnalytics();
        
        StringBuilder csv = new StringBuilder();
        // BOM for Excel
        csv.append('\uFEFF');
        
        csv.append("BÁO CÁO HIỆU QUẢ CHIẾN DỊCH MARKETING & VOUCHER\n");
        csv.append("Thời gian xuất:, ").append(java.time.LocalDateTime.now()).append("\n\n");
        
        csv.append("KPI TỔNG QUAN\n");
        csv.append("Tổng lượt sử dụng, Tổng doanh thu tạo ra, Tổng ngân sách giảm giá, ROI Tổng thể, AOV Tăng trưởng\n");
        csv.append(String.format("%d, %s, %s, %s%%, %s%%\n\n", 
            stats.getTotalUsageCount(),
            stats.getTotalRevenueGenerated().toString(),
            stats.getTotalDiscountBurned().toString(),
            stats.getOverallROI().toString(),
            stats.getAovLift().toString()));
            
        csv.append("CHI TIẾT HIỆU QUẢ TỪNG VOUCHER\n");
        csv.append("Mã Voucher, Loại, Trạng thái, Lượt dùng, Doanh thu, Giảm giá, ROI (%), Ngày bắt đầu, Ngày kết thúc\n");
        
        if (stats.getAllVouchers() != null) {
            for (VoucherAnalyticsDTO.VoucherMetric m : stats.getAllVouchers()) {
                csv.append(String.format("%s, %s, %s, %d, %s, %s, %s%%, %s, %s\n",
                    m.getCode(),
                    m.getCategory(),
                    m.getStatus(),
                    m.getUsageCount(),
                    m.getTotalRevenue().toString(),
                    m.getTotalDiscount().toString(),
                    m.getRoi().toString(),
                    m.getStartDate(),
                    m.getEndDate()));
            }
        }
        
        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", "marketing_analytics_report.csv");
        headers.set("Content-Type", "text/csv; charset=UTF-8");
        
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
}
