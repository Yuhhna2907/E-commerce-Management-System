package com.codegym.smartphonemanagement.controller;

import com.codegym.smartphonemanagement.model.dto.CouponResponseDTO;
import com.codegym.smartphonemanagement.model.dto.UserProfileDTO;
import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import com.codegym.smartphonemanagement.service.coupon.ICouponService;
import com.codegym.smartphonemanagement.service.loyalty.ILoyaltyPointService;
import com.codegym.smartphonemanagement.service.profile.IUserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice(basePackages = "com.codegym.smartphonemanagement.controller.user")
@RequiredArgsConstructor
public class GlobalModelController {

    private final ICouponService couponService;
    private final UserRepository userRepository;
    private final IUserProfileService userProfileService;
    private final ILoyaltyPointService loyaltyPointService;
    private final com.codegym.smartphonemanagement.service.wallet.WalletService walletService;

    /**
     * Helper method to get current authenticated user (returns null if not authenticated)
     */
    private User getCurrentUser(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        try {
            String username = authentication.getName();
            return userRepository.findByUsername(username).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    @ModelAttribute("GlobalVouchers")
    public List<CouponResponseDTO> globalVouchers() {
        return couponService.getAvailableCoupons().stream().map(c -> 
                CouponResponseDTO.builder()
                        .id(c.getId())
                        .code(c.getCode())
                        .discountType(c.getDiscountType())
                        .discountValue(c.getDiscountValue())
                        .maxDiscountAmount(c.getMaxDiscountAmount())
                        .minOrderValue(c.getMinOrderValue())
                        .startDate(c.getStartDate())
                        .endDate(c.getEndDate())
                        .build()
        ).collect(Collectors.toList());
    }

    @ModelAttribute("SavedVoucherCodes")
    public List<String> savedVoucherCodes(org.springframework.security.core.Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return List.of();
        }
        return couponService.getUserWallet(user).stream()
                .map(com.codegym.smartphonemanagement.model.Coupon::getCode)
                .collect(Collectors.toList());
    }

    @ModelAttribute("currentUser")
    public UserProfileDTO currentUser(org.springframework.security.core.Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return null;
        }
        try {
            return userProfileService.getProfile(user.getId());
        } catch (Exception e) {
            return null;
        }
    }

    @ModelAttribute("loyaltyPoints")
    public Integer loyaltyPoints(org.springframework.security.core.Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return 0;
        }
        try {
            return loyaltyPointService.getAccountInfo(user.getId()).getTotalPoints();
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("walletBalance")
    public java.math.BigDecimal walletBalance(org.springframework.security.core.Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return java.math.BigDecimal.ZERO;
        }
        try {
            return walletService.getWalletDTO(user.getId()).getBalance();
        } catch (Exception e) {
            return java.math.BigDecimal.ZERO;
        }
    }
}
