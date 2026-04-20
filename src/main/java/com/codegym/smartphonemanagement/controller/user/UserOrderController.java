package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.dto.BreadcrumbItem;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import com.codegym.smartphonemanagement.service.cart.user.ICartService;
import com.codegym.smartphonemanagement.service.cart.DTO.CartResponseDTO;
import com.codegym.smartphonemanagement.service.order.RefundService;
import com.codegym.smartphonemanagement.service.order.DTO.*;
import com.codegym.smartphonemanagement.service.order.user.IOrderService;
import com.codegym.smartphonemanagement.service.profile.IUserProfileService;
import com.codegym.smartphonemanagement.util.SecurityUtil;
import com.codegym.smartphonemanagement.model.PaymentMethod;
import com.codegym.smartphonemanagement.service.payment.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user/order")
public class UserOrderController {

    private final IOrderService orderService;
    private final ICartService cartService;
    private final RefundService refundService;
    private final IUserProfileService userProfileService;
    private final UserRepository userRepository;
    private final VNPayService vnPayService;
    private final com.codegym.smartphonemanagement.repository.user.UserRepository userRepository;

    /**
     * Helper method to get current authenticated user ID
     */
    private Long getCurrentUserId(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new com.codegym.smartphonemanagement.exception.UnauthorizedAccessException("User not authenticated");
        }
        String username = authentication.getName();
        com.codegym.smartphonemanagement.model.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.codegym.smartphonemanagement.exception.EntityNotFoundException("User not found"));
        return user.getId();
    }

    /**
     * Bước 1: Hiển thị trang điền thông tin thanh toán (Checkout)
     */
    @GetMapping("/checkout")
    public String showCheckoutPage(Model model, 
                                  jakarta.servlet.http.HttpSession session,
                                  org.springframework.security.core.Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        CartResponseDTO cart = cartService.getCart(userId);

        // Nếu giỏ hàng trống, không cho vào trang thanh toán
        if (cart == null || cart.getItems().isEmpty()) {
            return "redirect:/user/cart";
        }

        OrderRequestDTO requestDto = new OrderRequestDTO();
        
        String appliedCoupon = (String) session.getAttribute("APPLIED_COUPON");
        java.math.BigDecimal discountAmt = (java.math.BigDecimal) session.getAttribute("DISCOUNT_AMT");
        
        if (appliedCoupon != null) {
            requestDto.setCouponCode(appliedCoupon);
            model.addAttribute("appliedCouponCode", appliedCoupon);
            model.addAttribute("discountAmt", discountAmt);
            
            // Recalculate total for display
            java.math.BigDecimal currentTotal = cart.getTotalPrice().subtract(discountAmt);
            if (currentTotal.compareTo(java.math.BigDecimal.ZERO) < 0) {
                 currentTotal = java.math.BigDecimal.ZERO;
            }
            model.addAttribute("totalAfterDiscount", currentTotal);
        } else {
            model.addAttribute("totalAfterDiscount", cart.getTotalPrice());
        }

        model.addAttribute("cart", cart);
        model.addAttribute("orderRequest", requestDto);
        model.addAttribute("savedAddresses", userProfileService.getAddresses(userId));
        
        // Add breadcrumb navigation
        List<BreadcrumbItem> breadcrumbs = new ArrayList<>();
        breadcrumbs.add(BreadcrumbItem.builder()
                .label("Trang chủ")
                .url("/user/products")
                .active(false)
                .build());
        breadcrumbs.add(BreadcrumbItem.builder()
                .label("Giỏ hàng")
                .url("/user/cart")
                .active(false)
                .build());
        breadcrumbs.add(BreadcrumbItem.builder()
                .label("Thanh toán")
                .url(null)
                .active(true)
                .build());
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        return "user/order/checkout"; // Trả về file checkout.html
    }

    /**
     * Bước 2: Xử lý nút "Xác nhận đặt hàng" từ Form
     */
    @PostMapping("/place")
    public String placeOrder(@Valid @ModelAttribute("orderRequest") OrderRequestDTO orderDTO,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes,
                             HttpServletRequest request,
                             org.springframework.security.core.Authentication authentication,
                             Model model) {

        Long userId = getCurrentUserId(authentication);

        // 1. Kiểm tra nếu dính lỗi Validation (Trống tên, SĐT sai định dạng...)
        if (bindingResult.hasErrors()) {
            // Lấy lại giỏ hàng để hiển thị lại trang checkout nếu có lỗi
            CartResponseDTO cart = cartService.getCartByUserId(userId);
            model.addAttribute("cart", cart);
            // Trả về thẳng view checkout (không redirect để giữ message lỗi)
            return "user/order/checkout";
        }

        try {

            OrderResponseDTO savedOrder = orderService.createOrder(userId, orderDTO);

            if (orderDTO.getPaymentMethod() == PaymentMethod.VNPAY) {
                // Tạo URL thanh toán VNPAY và redirect
                String ipAddress = com.codegym.smartphonemanagement.config.payment.VNPAYConfig.getIpAddress(request);
                String paymentUrl = vnPayService.createPaymentUrl(savedOrder.getId(), savedOrder.getTotalPrice(), ipAddress);
                return "redirect:" + paymentUrl;
            }

            return "redirect:/user/order/success/" + savedOrder.getId();
        } catch (Exception e) {
            // Lỗi nghiệp vụ (ví dụ: đang thanh toán thì món đó bị người khác mua mất)
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/user/cart";
        }
    }

    /**
     * Bước 3: Trang thông báo đặt hàng thành công
     */
    @GetMapping("/success/{id}")
    public String showOrderSuccess(@PathVariable Long id, Model model) {
        OrderResponseDTO order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        return "user/order/success";
    }

    /**
     * Xem lịch sử mua hàng
     */
    @GetMapping("/history")
    public String showOrderHistory(Model model, org.springframework.security.core.Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<OrderResponseDTO> history = orderService.getOrderHistory(userId);
        model.addAttribute("orders", history);
        return "user/order/history";
    }

    /**
     * Xem chi tiết đơn hàng (timeline + sản phẩm)
     */
    @GetMapping("/detail/{id}")
    public String showOrderDetail(@PathVariable Long id, Model model, org.springframework.security.core.Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        OrderResponseDTO order = orderService.getOrderDetail(userId, id);
        model.addAttribute("order", order);
        return "user/order/detail";
    }

    /**
     * User hủy đơn (chỉ khi PENDING)
     */
    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id, RedirectAttributes redirectAttributes, org.springframework.security.core.Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        try {
            orderService.cancelOrder(userId, id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn hàng thành công. Kho hàng và voucher đã được hoàn lại.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/user/order/history";
    }

    /**
     * Trang form gửi yêu cầu refund
     */
    @GetMapping("/{id}/refund")
    public String showRefundForm(@PathVariable Long id, Model model, org.springframework.security.core.Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        OrderResponseDTO order = orderService.getOrderDetail(userId, id);
        if (!order.isCanRefund()) {
            return "redirect:/user/order/detail/" + id;
        }
        model.addAttribute("order", order);
        model.addAttribute("refundRequest", new RefundRequestDTO());
        return "user/order/refund_form";
    }

    /**
     * Submit yêu cầu refund
     */
    @PostMapping("/{id}/refund")
    public String submitRefund(@PathVariable Long id,
                               @ModelAttribute RefundRequestDTO refundDTO,
                               RedirectAttributes redirectAttributes,
                               org.springframework.security.core.Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        try {
            refundDTO.setOrderId(id);
            refundService.createRefundRequest(userId, refundDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Yêu cầu hoàn trả đã được gửi. Vui lòng chờ Admin duyệt.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/user/order/" + id + "/refund";
        }
        return "redirect:/user/order/detail/" + id;
    }

    @PostMapping("/reorder/{orderId}")
    public String reorder(@PathVariable Long orderId, RedirectAttributes redirectAttributes, org.springframework.security.core.Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        try {
            orderService.reorderOrderToCart(userId, orderId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm từ đơn hàng vào giỏ.");
            return "redirect:/user/cart";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/user/order/history";
        }
    }

    /**
     * API tĩnh lấy phí giao hàng theo Tỉnh/thành phục vụ Frontend (Ajax)
     */
    @GetMapping("/api/shipping-fee")
    @ResponseBody
    public java.math.BigDecimal calculateShippingFee(@RequestParam(value = "province", required = false) String province) {
        if (province == null || province.trim().isEmpty()) {
            return java.math.BigDecimal.valueOf(50000);
        }
        String lowerProv = province.toLowerCase();
        if (lowerProv.contains("hà nội") || lowerProv.contains("hồ chí minh") || lowerProv.contains("hcm") || lowerProv.contains("đà nẵng")) {
            return java.math.BigDecimal.valueOf(30000);
        }
        return java.math.BigDecimal.valueOf(50000);
    }
}