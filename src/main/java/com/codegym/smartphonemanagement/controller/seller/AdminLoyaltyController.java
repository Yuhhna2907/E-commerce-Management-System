package com.codegym.smartphonemanagement.controller.seller;

import com.codegym.smartphonemanagement.model.LoyaltyAccount;
import com.codegym.smartphonemanagement.model.MemberTier;
import com.codegym.smartphonemanagement.repository.user.LoyaltyAccountRepository;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import com.codegym.smartphonemanagement.service.loyalty.ILoyaltyPointService;
import com.codegym.smartphonemanagement.service.loyalty.LoyaltyPointService;
import com.codegym.smartphonemanagement.service.loyalty.dto.LoyaltyAccountDTO;
import com.codegym.smartphonemanagement.service.loyalty.dto.LoyaltyStatsDTO;
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/loyalty")
public class AdminLoyaltyController {

    private final LoyaltyPointService loyaltyPointService;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final UserRepository userRepository;

    /**
     * GET /admin/loyalty — Dashboard + danh sách tất cả user và điểm
     */
    @GetMapping
    public String listAll(@RequestParam(required = false) String search,
                          @RequestParam(required = false) String tier,
                          Model model) {
        List<com.codegym.smartphonemanagement.model.User> users = userRepository.findStandardUsers();

        // Filter theo keyword
        if (search != null && !search.isBlank()) {
            String kw = search.trim().toLowerCase();
            users = users.stream()
                    .filter(u -> {
                        String name = u.getFullName() != null ? u.getFullName().toLowerCase() : "";
                        String email = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
                        String username = u.getUsername() != null ? u.getUsername().toLowerCase() : "";
                        return name.contains(kw) || email.contains(kw) || username.contains(kw);
                    })
                    .collect(Collectors.toList());
        }

        // Map sang DTO (Bao gồm cả người chưa có account - sẽ tự tạo hoặc mặc định 0)
        List<LoyaltyAccountDTO> accountDTOs = users.stream()
                .map(u -> loyaltyPointService.getAccountInfo(u.getId()))
                .collect(Collectors.toList());

        // Filter theo Tier (Nếu có)
        if (tier != null && !tier.isBlank()) {
            try {
                MemberTier selectedTier = MemberTier.valueOf(tier.toUpperCase());
                accountDTOs = accountDTOs.stream()
                        .filter(a -> a.getTier() == selectedTier)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {}
        }

        // Stats aggregate
        LoyaltyStatsDTO stats = loyaltyPointService.getLoyaltyStats();

        // Tier counts from the list we will actually display
        long bronzeCount   = accountDTOs.stream().filter(a -> a.getTier() == MemberTier.BRONZE).count();
        long silverCount   = accountDTOs.stream().filter(a -> a.getTier() == MemberTier.SILVER).count();
        long goldCount     = accountDTOs.stream().filter(a -> a.getTier() == MemberTier.GOLD).count();
        long diamondCount  = accountDTOs.stream().filter(a -> a.getTier() == MemberTier.DIAMOND).count();

        model.addAttribute("accounts", accountDTOs);
        model.addAttribute("stats", stats);
        model.addAttribute("bronzeCount", bronzeCount);
        model.addAttribute("silverCount", silverCount);
        model.addAttribute("goldCount", goldCount);
        model.addAttribute("diamondCount", diamondCount);
        model.addAttribute("pageTitle", "loyalty");

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
        model.addAttribute("pageTitle", "loyalty");

        // User info
        userRepository.findById(userId).ifPresent(u -> model.addAttribute("targetUser", u));

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
                    String.format("Đã điều chỉnh %+d điểm cho user #%d thành công!", delta, userId));
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
