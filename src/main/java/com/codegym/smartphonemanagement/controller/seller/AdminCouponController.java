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
    private final com.codegym.smartphonemanagement.repository.user.CategoryRepository categoryRepository;

    @GetMapping
    public String listCoupons(Model model) {
        model.addAttribute("coupons", couponRepository.findAll());
        return "admin/coupon/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("coupon", new AdminCouponRequestDTO());
        model.addAttribute("allProducts", productRepository.findByActiveTrue(org.springframework.data.domain.Pageable.unpaged()).getContent());
        model.addAttribute("allCategories", categoryRepository.findAll());
        model.addAttribute("allPaymentMethods", com.codegym.smartphonemanagement.model.PaymentMethod.values());
        model.addAttribute("allCouponCategories", com.codegym.smartphonemanagement.model.CouponCategory.values());
        // Lấy danh sách Brand từ Product an toàn bằng API hoặc stream, tạm build cứng hoặc map:
        model.addAttribute("allBrands", productRepository.findAll().stream().map(Product::getBrand).filter(b -> b != null).distinct().toList());
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
        dto.setCouponCategory(coupon.getCouponCategory());
        dto.setApplicableCategoryIds(
                coupon.getApplicableCategories() == null ? List.of() :
                        coupon.getApplicableCategories().stream().map(com.codegym.smartphonemanagement.model.Category::getId).toList()
        );
        dto.setApplicableBrands(coupon.getApplicableBrands() == null ? List.of() : List.copyOf(coupon.getApplicableBrands()));
        dto.setApplicablePaymentMethods(coupon.getApplicablePaymentMethods() == null ? List.of() : List.copyOf(coupon.getApplicablePaymentMethods()));
        
        model.addAttribute("coupon", dto);
        model.addAttribute("allProducts", productRepository.findByActiveTrue(org.springframework.data.domain.Pageable.unpaged()).getContent());
        model.addAttribute("allCategories", categoryRepository.findAll());
        model.addAttribute("allPaymentMethods", com.codegym.smartphonemanagement.model.PaymentMethod.values());
        model.addAttribute("allCouponCategories", com.codegym.smartphonemanagement.model.CouponCategory.values());
        model.addAttribute("allBrands", productRepository.findAll().stream().map(Product::getBrand).filter(b -> b != null).distinct().toList());
        return "admin/coupon/form";
    }

    @PostMapping("/save")
    public String saveCoupon(@ModelAttribute AdminCouponRequestDTO dto) {
        if (dto.getStartDate() != null && dto.getEndDate() != null && dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu không thể sau ngày kết thúc.");
        }
        if (dto.getDiscountType() == com.codegym.smartphonemanagement.model.DiscountType.PERCENTAGE && dto.getDiscountValue() != null && dto.getDiscountValue().compareTo(new java.math.BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Giảm theo phần trăm không được vượt quá 100%.");
        }

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
        
        coupon.setCouponCategory(dto.getCouponCategory());
        if (dto.getApplicableCategoryIds() != null && !dto.getApplicableCategoryIds().isEmpty()) {
            coupon.setApplicableCategories(categoryRepository.findAllById(dto.getApplicableCategoryIds()));
        } else {
            coupon.setApplicableCategories(List.of());
        }
        coupon.setApplicableBrands(dto.getApplicableBrands() == null ? List.of() : dto.getApplicableBrands());
        coupon.setApplicablePaymentMethods(dto.getApplicablePaymentMethods() == null ? List.of() : dto.getApplicablePaymentMethods());

        couponRepository.save(coupon);
        return "redirect:/admin/coupons";
    }

    @GetMapping("/delete/{id}")
    public String deleteCoupon(@PathVariable Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid coupon Id:" + id));
        coupon.setStatus(com.codegym.smartphonemanagement.model.CouponStatus.CANCELLED);
        couponRepository.save(coupon);
        return "redirect:/admin/coupons";
    }

    @PostMapping("/toggle-status/{id}")
    public String toggleCouponStatus(@PathVariable Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid coupon Id:" + id));
        if (coupon.getStatus() == com.codegym.smartphonemanagement.model.CouponStatus.ACTIVE) {
            coupon.setStatus(com.codegym.smartphonemanagement.model.CouponStatus.PAUSED);
        } else if (coupon.getStatus() == com.codegym.smartphonemanagement.model.CouponStatus.PAUSED) {
            coupon.setStatus(com.codegym.smartphonemanagement.model.CouponStatus.ACTIVE);
        }
        couponRepository.save(coupon);
        return "redirect:/admin/coupons";
    }

    @GetMapping("/export")
    public org.springframework.http.ResponseEntity<byte[]> exportCoupons() {
        List<Coupon> coupons = couponRepository.findAll();
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("ID,Mã Code,Loại Giảm,Giá Trị,Đã dùng,Tối đa,Giới hạn 1 User,Ngày Bắt Đầu,Ngày Kết Thúc,Trạng Thái\n");
        for (Coupon c : coupons) {
            csvBuilder.append(c.getId()).append(",")
                    .append(c.getCode()).append(",")
                    .append(c.getDiscountType().name()).append(",")
                    .append(c.getDiscountValue()).append(",")
                    .append(c.getCurrentUsageGlobal()).append(",")
                    .append(c.getMaxUsageGlobal() == null ? "Không giới hạn" : c.getMaxUsageGlobal()).append(",")
                    .append(c.getPerUserLimit()).append(",")
                    .append(c.getStartDate()).append(",")
                    .append(c.getEndDate()).append(",")
                    .append(c.getStatus().name()).append("\n");
        }
        
        byte[] csvBytes = csvBuilder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        // Add BOM for Excel UTF-8 display
        byte[] bom = new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF};
        byte[] finalBytes = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, finalBytes, 0, bom.length);
        System.arraycopy(csvBytes, 0, finalBytes, bom.length, csvBytes.length);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=coupons_export_" + java.time.LocalDate.now() + ".csv");
        headers.setContentType(org.springframework.http.MediaType.parseMediaType("text/csv; charset=UTF-8"));

        return new org.springframework.http.ResponseEntity<>(finalBytes, headers, org.springframework.http.HttpStatus.OK);
    }
}
