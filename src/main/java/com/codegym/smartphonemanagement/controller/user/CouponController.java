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
        return "redirect:" + (referer != null ? referer : "/user/products");
    }

    @PostMapping("/apply")
    public String applyCoupon(@RequestParam("couponCode") String code, HttpSession session, HttpServletRequest request, RedirectAttributes redirectAttributes) {
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

        // Tạo order nháp từ giỏ hàng để validation
        Order orderDraft = new Order();
        orderDraft.setTotalPrice(cart.getTotalPrice());
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

        CouponValidationResult result = couponService.validateCoupon(code, user, orderDraft);
        
        if (result.isValid()) {
            session.setAttribute("APPLIED_COUPON", code);
            session.setAttribute("DISCOUNT_AMT", result.getDiscountAmount());
             redirectAttributes.addFlashAttribute("successCoupon", result.getMessage());
        } else {
             session.removeAttribute("APPLIED_COUPON");
             session.removeAttribute("DISCOUNT_AMT");
             redirectAttributes.addFlashAttribute("errorCoupon", result.getMessage());
        }
        
        return redirectUrl;
    }
}
