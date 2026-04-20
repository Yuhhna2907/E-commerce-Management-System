package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.model.*;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import com.codegym.smartphonemanagement.service.cart.DTO.CartItemResponseDTO;
import com.codegym.smartphonemanagement.service.cart.DTO.CartResponseDTO;
import com.codegym.smartphonemanagement.service.cart.user.ICartService;
import com.codegym.smartphonemanagement.service.coupon.CouponValidationResult;
import com.codegym.smartphonemanagement.service.coupon.ICouponService;
import com.codegym.smartphonemanagement.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/user/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final ICouponService couponService;
    private final ICartService cartService;
    private final UserRepository userRepository;

    // Lấy userId từ SecurityContext
    private Long getCurrentUserId() {
        return SecurityUtil.getCurrentUserId(userRepository);
    }

    @GetMapping("/wallet")
    public String showWalletPage(org.springframework.ui.Model model) {
        User user = userRepository.findById(getCurrentUserId()).orElse(null);
        if (user != null) {
            model.addAttribute("myWalletCoupons", couponService.getUserWallet(user));
        }
        return "user/coupon/wallet";
    }

    @PostMapping("/save")
    public String saveToWallet(@RequestParam("code") String code, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findById(getCurrentUserId()).orElseThrow(() -> new RuntimeException("User not found"));
            couponService.saveToWallet(user, code);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu mã giảm giá thành công: " + code);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/user/product/list");
    }

    @PostMapping("/apply")
    public String applyCoupon(@RequestParam(value = "couponCode", required = false) String code,
                              @RequestParam(value = "shippingCouponCode", required = false) String shippingCode,
                              HttpSession session, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String referer = request.getHeader("Referer");
        String redirectUrl = "redirect:/user/order/checkout";
        if (referer != null && referer.contains("/user/cart")) {
            redirectUrl = "redirect:/user/cart";
        }

        if (code == null || code.trim().isEmpty()) {
            session.removeAttribute("APPLIED_COUPON");
            session.removeAttribute("DISCOUNT_AMT");
            redirectAttributes.addFlashAttribute("errorCoupon", "Vui lòng nhập hoặc hủy mã giảm giá.");
            return redirectUrl;
        }

        User user = userRepository.findById(getCurrentUserId()).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorCoupon", "Vui lòng đăng nhập.");
            return redirectUrl;
        }

        CartResponseDTO cart = cartService.getCart(getCurrentUserId());
        if (cart == null || cart.getItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorCoupon", "Giỏ hàng trống.");
            return redirectUrl;
        }

        Order orderDraft = new Order();
        orderDraft.setTotalPrice(cart.getTotalPrice());
        orderDraft.setShippingFee(new BigDecimal("35000")); // Fake shipping fee for validation purposes
        List<OrderItem> items = new ArrayList<>();
        for (CartItemResponseDTO cartItem : cart.getItems()) {
            OrderItem item = new OrderItem();
            item.setQuantity(cartItem.getQuantity());
            item.setPrice(cartItem.getPrice());
            Product p = new Product();
            p.setId(cartItem.getProductId());
            item.setProduct(p);
            items.add(item);
        }
        orderDraft.setItems(items);

        boolean hasSuccess = false;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder errorMsg = new StringBuilder();

        // Xử lý mã Freeship
        if (shippingCode == null || shippingCode.trim().isEmpty()) {
            session.removeAttribute("APPLIED_SHIPPING_COUPON");
            session.removeAttribute("SHIPPING_DISCOUNT_AMT");
        } else {
            CouponValidationResult shipResult = couponService.validateCoupon(shippingCode, user, orderDraft);
            if (shipResult.isValid()) {
                session.setAttribute("APPLIED_SHIPPING_COUPON", shippingCode);
                session.setAttribute("SHIPPING_DISCOUNT_AMT", shipResult.getDiscountAmount());
                successMsg.append("- ").append(shipResult.getMessage()).append(" (Freeship)\n");
                hasSuccess = true;
            } else {
                session.removeAttribute("APPLIED_SHIPPING_COUPON");
                session.removeAttribute("SHIPPING_DISCOUNT_AMT");
                errorMsg.append("- ").append(shipResult.getMessage()).append(" (Freeship)\n");
            }
        }

        // Xử lý mã Giảm giá sản phẩm
        if (code == null || code.trim().isEmpty()) {
            session.removeAttribute("APPLIED_COUPON");
            session.removeAttribute("DISCOUNT_AMT");
        } else {
            CouponValidationResult prodResult = couponService.validateCoupon(code, user, orderDraft);
            if (prodResult.isValid()) {
                session.setAttribute("APPLIED_COUPON", code);
                session.setAttribute("DISCOUNT_AMT", prodResult.getDiscountAmount());
                successMsg.append("- ").append(prodResult.getMessage()).append(" (Đơn hàng)\n");
                hasSuccess = true;
            } else {
                session.removeAttribute("APPLIED_COUPON");
                session.removeAttribute("DISCOUNT_AMT");
                errorMsg.append("- ").append(prodResult.getMessage()).append(" (Đơn hàng)\n");
            }
        }

        if (hasSuccess) {
            redirectAttributes.addFlashAttribute("successCoupon", successMsg.toString());
        }
        if (!errorMsg.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorCoupon", errorMsg.toString());
        }

        return redirectUrl;
    }
}
