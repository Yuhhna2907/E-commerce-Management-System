package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.model.SzWithdrawalRequest;
import com.codegym.smartphonemanagement.service.wallet.WalletService;
import com.codegym.smartphonemanagement.service.wallet.dto.WalletDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/user/wallet")
public class UserWalletController {

    private final WalletService walletService;
    private final com.codegym.smartphonemanagement.repository.user.UserRepository userRepository;

    /**
     * GET /user/wallet — Trang ví SmartZone Xu của user
     */
    @GetMapping
    public String myWallet(org.springframework.security.core.Authentication authentication,
                           @RequestParam(defaultValue = "0") int page,
                           Model model) {
        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        Long userId = getUserId(authentication);
        WalletDTO wallet = walletService.getWalletDTO(userId);
        Page<?> transactions = walletService.getTransactions(userId, PageRequest.of(page, 10));

        // Yêu cầu rút tiền của user
        var myRequests = walletService.getPendingWithdrawals().stream()
                .filter(r -> r.getUser().getId().equals(userId))
                .map(walletService::toWithdrawalDTO)
                .toList();

        model.addAttribute("wallet", wallet);
        model.addAttribute("transactions", transactions);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactions.getTotalPages());
        model.addAttribute("myRequests", myRequests);
        model.addAttribute("userId", userId);
        return "user/wallet/index";
    }

    /**
     * POST /user/wallet/withdraw — User tạo yêu cầu rút tiền
     */
    @PostMapping("/withdraw")
    public String requestWithdrawal(org.springframework.security.core.Authentication authentication,
                                    @RequestParam BigDecimal amount,
                                    @RequestParam String bankName,
                                    @RequestParam String bankAccount,
                                    @RequestParam String bankHolder,
                                    @RequestParam(required = false) String userNote,
                                    RedirectAttributes ra) {
        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";
        Long userId = getUserId(authentication);
        try {
            SzWithdrawalRequest req = walletService.requestWithdrawal(
                    userId, amount, bankName, bankAccount, bankHolder, userNote);
            ra.addFlashAttribute("successMessage",
                    String.format("Yêu cầu rút %,.0f₫ đã được gửi! Mã yêu cầu: #%d. Admin sẽ xử lý trong 24h.", amount, req.getId()));
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/user/wallet";
    }

    // ==================== PRIVATE HELPERS ====================

    /**
     * Lấy userId từ Authentication
     */
    private Long getUserId(org.springframework.security.core.Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new com.codegym.smartphonemanagement.exception.EntityNotFoundException("User not found"))
                .getId();
    }
}
