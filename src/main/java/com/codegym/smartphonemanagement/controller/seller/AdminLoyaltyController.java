package com.codegym.smartphonemanagement.controller.seller;

import com.codegym.smartphonemanagement.model.LoyaltyAccount;
import com.codegym.smartphonemanagement.repository.user.LoyaltyAccountRepository;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import com.codegym.smartphonemanagement.service.loyalty.ILoyaltyPointService;
import com.codegym.smartphonemanagement.service.loyalty.dto.LoyaltyAccountDTO;
import com.codegym.smartphonemanagement.service.loyalty.dto.PointTransactionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/loyalty")
public class AdminLoyaltyController {

    private final ILoyaltyPointService loyaltyPointService;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final UserRepository userRepository;

    /**
     * GET /admin/loyalty — Danh sách tất cả user và điểm
     */
    @GetMapping
    public String listAll(Model model) {
        List<LoyaltyAccount> accounts = loyaltyAccountRepository.findAll();
        model.addAttribute("accounts", accounts);
        return "admin/loyalty/list";
    }

    /**
     * GET /admin/loyalty/{userId} — Chi tiết điểm của 1 user + lịch sử giao dịch
     */
    @GetMapping("/{userId}")
    public String userDetail(@PathVariable Long userId,
                             @RequestParam(defaultValue = "0") int page,
                             Model model) {
        LoyaltyAccountDTO account = loyaltyPointService.getOrCreateAccount(userId);
        Page<PointTransactionDTO> transactions = loyaltyPointService
                .getTransactionHistory(userId, PageRequest.of(page, 15));

        model.addAttribute("targetUserId", userId);
        model.addAttribute("account", account);
        model.addAttribute("transactions", transactions);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactions.getTotalPages());
        return "admin/loyalty/detail";
    }

    /**
     * POST /admin/loyalty/{userId}/adjust — Điều chỉnh điểm thủ công
     */
    @PostMapping("/{userId}/adjust")
    public String adjustPoints(@PathVariable Long userId,
                               @RequestParam int delta,
                               @RequestParam(required = false) String reason,
                               RedirectAttributes redirectAttributes) {
        try {
            loyaltyPointService.adminAdjustPoints(userId, delta, reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Đã điều chỉnh %+d điểm cho user #%d", delta, userId));
        } catch (RuntimeException e) {
            log.warn("Admin adjust points failed for user {}: {}", userId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/loyalty/" + userId;
    }

    /**
     * GET /admin/loyalty/{userId}/account — JSON (dùng cho AJAX)
     */
    @GetMapping("/{userId}/account")
    @ResponseBody
    public ResponseEntity<LoyaltyAccountDTO> getAccountJson(@PathVariable Long userId) {
        return ResponseEntity.ok(loyaltyPointService.getAccountInfo(userId));
    }
}
