package com.codegym.smartphonemanagement.controller.admin;

import com.codegym.smartphonemanagement.model.RefundStatus;
import com.codegym.smartphonemanagement.service.admin.AdminRefundService;
import com.codegym.smartphonemanagement.service.admin.dto.AdminRefundDTO;
import com.codegym.smartphonemanagement.service.admin.dto.RefundStatsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/refunds")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRefundController {

    private final AdminRefundService adminRefundService;

    private static final int PAGE_SIZE = 10;

    @GetMapping
    public String showRefundCenter(
            @RequestParam(required = false) RefundStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Model model) {

        // 1. Stats with date range filter
        RefundStatsDTO stats = adminRefundService.getRefundStats(dateFrom, dateTo);

        // 2. Paginated + status-filtered refund list
        Page<AdminRefundDTO> refundPage = adminRefundService.getRefundsPaginated(status, page, PAGE_SIZE);

        model.addAttribute("stats", stats);
        model.addAttribute("refunds", refundPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", refundPage.getTotalPages());
        model.addAttribute("totalElements", refundPage.getTotalElements());
        model.addAttribute("currentStatus", status);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);

        return "admin/refund/index";
    }

    @PostMapping("/{id}/approve")
    public String approveRefund(@PathVariable Long id,
                                @RequestParam(required = false) String adminNote,
                                RedirectAttributes redirectAttributes) {
        try {
            adminRefundService.approveRefund(id, adminNote);
            redirectAttributes.addFlashAttribute("success",
                    "Đã duyệt đơn hoàn tiền #" + id + " thành công! Số tiền đã quy đổi thành Điểm Thưởng. 📧 Email thông báo đã được gửi cho khách hàng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/refunds";
    }

    @PostMapping("/{id}/reject")
    public String rejectRefund(@PathVariable Long id,
                               @RequestParam String adminNote,
                               RedirectAttributes redirectAttributes) {
        try {
            adminRefundService.rejectRefund(id, adminNote);
            redirectAttributes.addFlashAttribute("success",
                    "Đã từ chối đơn hoàn tiền #" + id + ". 📧 Email thông báo đã được gửi cho khách hàng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/refunds";
    }
}
