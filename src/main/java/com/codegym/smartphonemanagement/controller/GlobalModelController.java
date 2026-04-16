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

    private static final Long MOCK_USER_ID = 1L; // thay bằng Security principal sau

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
    public List<String> savedVoucherCodes() {
        // Hardcode mock user ID for now
        User user = userRepository.findById(MOCK_USER_ID).orElse(null);
        if (user == null) {
            return List.of();
        }
        return couponService.getUserWallet(user).stream()
                .map(com.codegym.smartphonemanagement.model.Coupon::getCode)
                .collect(Collectors.toList());
    }

    @ModelAttribute("currentUser")
    public UserProfileDTO currentUser() {
        try {
            return userProfileService.getProfile(MOCK_USER_ID);
        } catch (Exception e) {
            return null;
        }
    }

    @ModelAttribute("loyaltyPoints")
    public Integer loyaltyPoints() {
        try {
            return loyaltyPointService.getAccountInfo(MOCK_USER_ID).getTotalPoints();
        } catch (Exception e) {
            return 0;
        }
    }
}
