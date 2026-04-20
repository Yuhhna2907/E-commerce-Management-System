package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import com.codegym.smartphonemanagement.service.loyalty.ILoyaltyPointService;
import com.codegym.smartphonemanagement.service.loyalty.dto.LoyaltyAccountDTO;
import com.codegym.smartphonemanagement.service.loyalty.dto.PointTransactionDTO;
import com.codegym.smartphonemanagement.service.loyalty.dto.RedeemRequestDTO;
import com.codegym.smartphonemanagement.service.loyalty.dto.RedeemResultDTO;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/user/loyalty")
public class UserLoyaltyController {

    private final ILoyaltyPointService loyaltyPointService;
    private final UserRepository userRepository;

    /**
     * GET /user/loyalty — Trang Loyalty Dashboard (Thymeleaf)
     */
    @GetMapping
    public String loyaltyDashboard(Model model,
                                   Authentication authentication,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId(authentication);
        LoyaltyAccountDTO account = loyaltyPointService.getOrCreateAccount(userId);
        Page<PointTransactionDTO> transactions = loyaltyPointService
                .getTransactionHistory(userId, PageRequest.of(page, size));

        model.addAttribute("account", account);
        model.addAttribute("transactions", transactions);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactions.getTotalPages());
        model.addAttribute("minRedeemPoints", 100);
        return "user/loyalty/index";
    }

    /**
     * GET /api/user/loyalty/account — JSON: thông tin tài khoản điểm
     */
    @GetMapping("/account")
    @ResponseBody
    public ResponseEntity<LoyaltyAccountDTO> getAccount(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(loyaltyPointService.getAccountInfo(userId));
    }

    /**
     * GET /api/user/loyalty/transactions — JSON: lịch sử (phân trang)
     */
    @GetMapping("/transactions")
    @ResponseBody
    public ResponseEntity<Page<PointTransactionDTO>> getTransactions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(
                loyaltyPointService.getTransactionHistory(userId, PageRequest.of(page, size)));
    }

    /**
     * POST /user/loyalty/redeem — Đổi điểm lấy coupon
     */
    @PostMapping("/redeem")
    @ResponseBody
    public ResponseEntity<?> redeemPoints(@Valid @RequestBody RedeemRequestDTO request,
                                         Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        try {
            RedeemResultDTO result = loyaltyPointService.redeemPoints(userId, request.getPoints());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.warn("Redeem points failed for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Helper method to get current user ID from authentication
     */
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        return user.getId();
    }
}
