package com.codegym.smartphonemanagement.controller;

import com.codegym.smartphonemanagement.model.dto.CouponResponseDTO;
import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import com.codegym.smartphonemanagement.service.coupon.ICouponService;
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
        User user = userRepository.findById(1L).orElse(null);
        if (user == null) {
            return List.of();
        }
        return couponService.getUserWallet(user).stream()
                .map(com.codegym.smartphonemanagement.model.Coupon::getCode)
                .collect(Collectors.toList());
    }
}
