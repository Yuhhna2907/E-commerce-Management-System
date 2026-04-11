package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.service.cart.user.ICartService;
import com.codegym.smartphonemanagement.service.cart.DTO.CartResponseDTO;
import com.codegym.smartphonemanagement.service.order.DTO.OrderRequestDTO;
import com.codegym.smartphonemanagement.service.order.DTO.OrderResponseDTO;
import com.codegym.smartphonemanagement.service.order.user.IOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user/order")
public class UserOrderController {

    private final IOrderService orderService;
    private final ICartService cartService;

    // Giả lập ID người dùng (Duy thay bằng Security context sau nhé)
    private final Long USER_ID = 1L;

    /**
     * Bước 1: Hiển thị trang điền thông tin thanh toán (Checkout)
     */
    @GetMapping("/checkout")
    public String showCheckoutPage(Model model, jakarta.servlet.http.HttpSession session) {
        CartResponseDTO cart = cartService.getCart(USER_ID);

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
        return "user/order/checkout"; // Trả về file checkout.html
    }

    /**
     * Bước 2: Xử lý nút "Xác nhận đặt hàng" từ Form
     */
    @PostMapping("/place")
    public String placeOrder(@Valid @ModelAttribute("orderRequest") OrderRequestDTO orderDTO,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes,
                             Model model) {

        // 1. Kiểm tra nếu dính lỗi Validation (Trống tên, SĐT sai định dạng...)
        if (bindingResult.hasErrors()) {
            // Lấy lại giỏ hàng để hiển thị lại trang checkout nếu có lỗi
            CartResponseDTO cart = cartService.getCartByUserId(USER_ID);
            model.addAttribute("cart", cart);
            // Trả về thẳng view checkout (không redirect để giữ message lỗi)
            return "user/order/checkout";
        }

        try {

            OrderResponseDTO savedOrder = orderService.createOrder(USER_ID, orderDTO);

            return "redirect:/user/order/success/" + savedOrder.getId();
        } catch (Exception e) {
            // Lỗi nghiệp vụ (ví dụ: đang thanh toán thì món đó bị người khác mua mất)
            redirectAttributes.addFlashAttribute("error", e.getMessage());
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
    public String showOrderHistory(Model model) {
        List<OrderResponseDTO> history = orderService.getOrderHistory(USER_ID);
        model.addAttribute("orders", history);
        return "user/order/history";
    }

    @PostMapping("/reorder/{orderId}")
    public String reorder(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        try {
            orderService.reorderOrderToCart(USER_ID, orderId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm từ đơn hàng vào giỏ.");
            return "redirect:/user/cart";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/user/order/history";
        }
    }
}