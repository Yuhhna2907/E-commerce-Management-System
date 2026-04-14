package com.codegym.smartphonemanagement.controller.seller;

import com.codegym.smartphonemanagement.model.Coupon;
import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.model.dto.AdminCouponRequestDTO;
import com.codegym.smartphonemanagement.repository.CouponRepository;
import com.codegym.smartphonemanagement.repository.seller.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponRepository couponRepository;
    private final ProductRepository productRepository;

    @GetMapping
    public String listCoupons(Model model) {
        model.addAttribute("coupons", couponRepository.findAll());
        return "admin/coupon/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("coupon", new AdminCouponRequestDTO());
        model.addAttribute("allProducts", productRepository.findByActiveTrue(org.springframework.data.domain.Pageable.unpaged()).getContent());
        return "admin/coupon/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid coupon Id:" + id));
        
        AdminCouponRequestDTO dto = new AdminCouponRequestDTO();
        dto.setId(coupon.getId());
        dto.setCode(coupon.getCode());
        dto.setDiscountType(coupon.getDiscountType());
        dto.setDiscountValue(coupon.getDiscountValue());
        dto.setMaxDiscountAmount(coupon.getMaxDiscountAmount());
        dto.setMinOrderValue(coupon.getMinOrderValue());
        dto.setMaxUsageGlobal(coupon.getMaxUsageGlobal());
        dto.setPerUserLimit(coupon.getPerUserLimit());
        dto.setStartDate(coupon.getStartDate());
        dto.setEndDate(coupon.getEndDate());
        dto.setDescription(coupon.getDescription());
        dto.setApplicableProductIds(
                coupon.getApplicableProducts() == null ? List.of() :
                        coupon.getApplicableProducts().stream().map(Product::getId).toList()
        );
        
        model.addAttribute("coupon", dto);
        model.addAttribute("allProducts", productRepository.findByActiveTrue(org.springframework.data.domain.Pageable.unpaged()).getContent());
        return "admin/coupon/form";
    }

    @PostMapping("/save")
    public String saveCoupon(@ModelAttribute AdminCouponRequestDTO dto) {
        Coupon coupon;
        if (dto.getId() != null) {
            coupon = couponRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid coupon Id:" + dto.getId()));
            coupon.setStatus(com.codegym.smartphonemanagement.model.CouponStatus.ACTIVE);
            // Updating an existing coupon preserves usages but modifies rules.
        } else {
            coupon = new Coupon();
            coupon.setStatus(com.codegym.smartphonemanagement.model.CouponStatus.ACTIVE);
            coupon.setCurrentUsageGlobal(0);
        }

        coupon.setCode(dto.getCode());
        coupon.setDiscountType(dto.getDiscountType());
        coupon.setDiscountValue(dto.getDiscountValue());
        coupon.setMaxDiscountAmount(dto.getMaxDiscountAmount());
        coupon.setMinOrderValue(dto.getMinOrderValue());
        coupon.setMaxUsageGlobal(dto.getMaxUsageGlobal());
        coupon.setPerUserLimit(dto.getPerUserLimit());
        coupon.setStartDate(dto.getStartDate());
        coupon.setEndDate(dto.getEndDate());
        coupon.setDescription(dto.getDescription());
        if (dto.getApplicableProductIds() != null && !dto.getApplicableProductIds().isEmpty()) {
            coupon.setApplicableProducts(productRepository.findAllById(dto.getApplicableProductIds()));
        } else {
            coupon.setApplicableProducts(List.of());
        }

        couponRepository.save(coupon);
        return "redirect:/admin/coupons";
    }

    @GetMapping("/delete/{id}")
    public String deleteCoupon(@PathVariable Long id) {
        couponRepository.deleteById(id);
        return "redirect:/admin/coupons";
    }
}
