package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.service.loyalty.ILoyaltyPointService;
import com.codegym.smartphonemanagement.service.loyalty.dto.LoyaltyAccountDTO;
import com.codegym.smartphonemanagement.service.loyalty.dto.PointTransactionDTO;
import com.codegym.smartphonemanagement.service.loyalty.dto.RedeemRequestDTO;
import com.codegym.smartphonemanagement.service.loyalty.dto.RedeemResultDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
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

    // Hardcode mock — thay bằng Spring Security sau
    private static final Long MOCK_USER_ID = 1L;

    /**
     * GET /user/loyalty — Trang Loyalty Dashboard (Thymeleaf)
     */
    @GetMapping
    public String loyaltyDashboard(Model model,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        LoyaltyAccountDTO account = loyaltyPointService.getOrCreateAccount(MOCK_USER_ID);
        Page<PointTransactionDTO> transactions = loyaltyPointService
                .getTransactionHistory(MOCK_USER_ID, PageRequest.of(page, size));

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
    public ResponseEntity<LoyaltyAccountDTO> getAccount() {
        return ResponseEntity.ok(loyaltyPointService.getAccountInfo(MOCK_USER_ID));
    }

    /**
     * GET /api/user/loyalty/transactions — JSON: lịch sử (phân trang)
     */
    @GetMapping("/transactions")
    @ResponseBody
    public ResponseEntity<Page<PointTransactionDTO>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                loyaltyPointService.getTransactionHistory(MOCK_USER_ID, PageRequest.of(page, size)));
    }

    /**
     * POST /user/loyalty/redeem — Đổi điểm lấy coupon
     */
    @PostMapping("/redeem")
    @ResponseBody
    public ResponseEntity<?> redeemPoints(@Valid @RequestBody RedeemRequestDTO request) {
        try {
            RedeemResultDTO result = loyaltyPointService.redeemPoints(MOCK_USER_ID, request.getPoints());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.warn("Redeem points failed for user {}: {}", MOCK_USER_ID, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
