package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.service.cart.user.ICartService;
import com.codegym.smartphonemanagement.service.cart.DTO.CartResponseDTO;
import com.codegym.smartphonemanagement.service.order.DTO.OrderResponseDTO;
import com.codegym.smartphonemanagement.service.order.user.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
    public String showCheckoutPage(Model model) {
        CartResponseDTO cart = cartService.getCart(USER_ID);

        // Nếu giỏ hàng trống, không cho vào trang thanh toán
        if (cart == null || cart.getItems().isEmpty()) {
            return "redirect:/user/cart";
        }

        model.addAttribute("cart", cart);
        return "user/order/checkout"; // Trả về file checkout.html
    }

    /**
     * Bước 2: Xử lý nút "Xác nhận đặt hàng" từ Form
     */
    @PostMapping("/place")
    public String placeOrder(@RequestParam String receiverName,
                             @RequestParam String receiverPhone,
                             @RequestParam String shippingAddress,
                             @RequestParam(required = false) String note,
                             RedirectAttributes redirectAttributes) {
        try {
            // Gọi Service xử lý logic nghiệp vụ (Trừ kho, lưu đơn, xóa giỏ)
            OrderResponseDTO order = orderService.createOrder(
                    USER_ID,
                    receiverName,
                    receiverPhone,
                    shippingAddress,
                    note
            );

            // Chuyển hướng sang trang thành công
            return "redirect:/user/order/success/" + order.getId();
        } catch (Exception e) {
            // Nếu lỗi (hết hàng...), báo lỗi về trang giỏ hàng
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
}