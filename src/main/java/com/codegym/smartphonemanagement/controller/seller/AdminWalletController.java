package com.codegym.smartphonemanagement.controller.seller;

import com.codegym.smartphonemanagement.model.SzWallet;
import com.codegym.smartphonemanagement.model.SzWithdrawalRequest;
import com.codegym.smartphonemanagement.model.WithdrawalStatus;
import com.codegym.smartphonemanagement.repository.SzWalletRepository;
import com.codegym.smartphonemanagement.service.wallet.WalletService;
import com.codegym.smartphonemanagement.service.wallet.dto.WalletDTO;
import com.codegym.smartphonemanagement.service.wallet.dto.WalletStatsDTO;
import com.codegym.smartphonemanagement.service.wallet.dto.WithdrawalRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;

import com.codegym.smartphonemanagement.service.wallet.WalletReportService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/wallet")
public class AdminWalletController {

    private final WalletService walletService;
    private final SzWalletRepository walletRepo;
    private final WalletReportService reportService;

    /**
     * GET /admin/wallet — Dashboard tổng quan Ví SmartZone Xu
     */
    @GetMapping
    public String dashboard(Model model) {
        WalletStatsDTO stats = walletService.getAdminStats();

        // Danh sách yêu cầu rút tiền đang PENDING
        List<WithdrawalRequestDTO> pendingWithdrawals = walletService.getPendingWithdrawals()
                .stream().map(walletService::toWithdrawalDTO).collect(Collectors.toList());

        // Feed giao dịch gần nhất
        var recentTxs = walletService.getRecentGlobalTransactions();

        model.addAttribute("stats", stats);
        model.addAttribute("pendingWithdrawals", pendingWithdrawals);
        model.addAttribute("recentTxs", recentTxs);
        model.addAttribute("pageTitle", "wallet");
        return "admin/wallet/dashboard";
    }

    /**
     * GET /admin/wallet/users — Danh sách tất cả ví
     */
    @GetMapping("/users")
    public String listWallets(@RequestParam(required = false) String search, Model model) {
        List<SzWallet> wallets = walletRepo.findAll();

        if (search != null && !search.isBlank()) {
            String kw = search.trim().toLowerCase();
            wallets = wallets.stream()
                    .filter(w -> {
                        String name = w.getUser().getFullName() != null ? w.getUser().getFullName().toLowerCase() : "";
                        String email = w.getUser().getEmail() != null ? w.getUser().getEmail().toLowerCase() : "";
                        return name.contains(kw) || email.contains(kw);
                    }).collect(Collectors.toList());
        }

        List<WalletDTO> walletDTOs = wallets.stream()
                .map(walletService::toDTO).collect(Collectors.toList());

        model.addAttribute("wallets", walletDTOs);
        model.addAttribute("search", search);
        model.addAttribute("pageTitle", "wallet");
        return "admin/wallet/users";
    }

    /**
     * GET /admin/wallet/users/{userId} — Ví chi tiết + lịch sử của một user
     */
    @GetMapping("/users/{userId}")
    public String userDetail(@PathVariable Long userId,
                             @RequestParam(defaultValue = "0") int page,
                             Model model) {
        WalletDTO wallet = walletService.getWalletDTO(userId);
        Page<?> transactions = walletService.getTransactions(userId, PageRequest.of(page, 15));

        model.addAttribute("wallet", wallet);
        model.addAttribute("transactions", transactions);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactions.getTotalPages());
        model.addAttribute("targetUserId", userId);
        model.addAttribute("pageTitle", "wallet");
        return "admin/wallet/user-detail";
    }

    /**
     * POST /admin/wallet/users/{userId}/topup — Admin nạp tiền vào ví
     */
    @PostMapping("/users/{userId}/topup")
    public String topUp(@PathVariable Long userId,
                        @RequestParam BigDecimal amount,
                        @RequestParam(required = false) String adminNote,
                        RedirectAttributes ra) {
        try {
            walletService.adminTopUp(userId, amount, adminNote);
            ra.addFlashAttribute("successMessage",
                    String.format("Đã nạp %,.0f₫ vào SmartZone Xu của user #%d!", amount, userId));
        } catch (Exception e) {
            log.warn("Admin topup failed for user {}: {}", userId, e.getMessage());
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/wallet/users/" + userId;
    }

    /**
     * POST /admin/wallet/withdrawals/{id}/approve — Duyệt rút tiền
     */
    @PostMapping("/withdrawals/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(required = false) String adminNote,
                          RedirectAttributes ra) {
        try {
            walletService.approveWithdrawal(id, adminNote);
            ra.addFlashAttribute("successMessage", "Đã duyệt yêu cầu rút tiền #" + id + " ✅");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/wallet";
    }

    /**
     * POST /admin/wallet/withdrawals/{id}/reject — Từ chối rút tiền
     */
    @PostMapping("/withdrawals/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(required = false) String adminNote,
                         RedirectAttributes ra) {
        try {
            walletService.rejectWithdrawal(id, adminNote);
            ra.addFlashAttribute("successMessage", "Đã từ chối yêu cầu rút tiền #" + id + " và hoàn tiền vào ví!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/wallet";
    }

    /**
     * GET /admin/wallet/reports/excel — Xuất báo cáo Excel theo khoảng thời gian
     */
    @GetMapping("/reports/excel")
    public ResponseEntity<byte[]> exportExcel(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            
            byte[] data = reportService.generateExcelReport(start, end);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Bao-cao-vi-" + startDate + "-to-" + endDate + ".xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            log.error("Lỗi khi xuất báo cáo Excel: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /admin/wallet/reports/pdf — Xuất báo cáo PDF theo khoảng thời gian
     */
    @GetMapping("/reports/pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            
            byte[] data = reportService.generatePdfReport(start, end);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Bao-cao-vi-" + startDate + "-to-" + endDate + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(data);
        } catch (Exception e) {
            log.error("Lỗi khi xuất báo cáo PDF: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
