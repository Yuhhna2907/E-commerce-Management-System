package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.service.order.DTO.OrderResponseDTO;
import com.codegym.smartphonemanagement.service.order.user.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user/order")
public class UserOrderController {
    private final IOrderService orderService;

    // Giả lập ID người dùng (Sau này dùng Spring Security để lấy Principal)
    private final Long USER_ID = 1L;

    // Xử lý bấm nút "Thanh toán" từ trang Giỏ hàng
    @PostMapping("/checkout")
    public String handleCheckout() {
        try {
            OrderResponseDTO order = orderService.checkout(USER_ID);
            return "redirect:/user/order/success/" + order.getId();
        } catch (Exception e) {
            // Nếu lỗi (hết hàng, giỏ trống...), quay lại giỏ kèm thông báo
            return "redirect:/user/cart?error=" + e.getMessage();
        }
    }

    // Trang thông báo đặt hàng thành công
    @GetMapping("/success/{id}")
    public String showOrderSuccess(@PathVariable Long id,
                                   Model model) {
        OrderResponseDTO order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        return "user/order/success";
    }

    // Trang xem lại lịch sử các đơn đã mua
    @GetMapping("/history")
    public String showOrderHistory(Model model) {
        List<OrderResponseDTO> history = orderService.getOrderHistory(USER_ID);
        model.addAttribute("orders", history);
        return "user/order/history";
    }
}
