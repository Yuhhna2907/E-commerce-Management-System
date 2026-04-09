package com.codegym.smartphonemanagement.controller.seller;

import com.codegym.smartphonemanagement.model.OrderStatus;
import com.codegym.smartphonemanagement.service.order.user.OrderService;
import com.codegym.smartphonemanagement.service.order.DTO.OrderResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public String listOrders(@RequestParam(required = false) OrderStatus status, Model model) {
        List<OrderResponseDTO> orders;
        if (status != null) {
            orders = orderService.getOrdersByStatus(status);
        } else {
            orders = orderService.getAllOrdersForAdmin();
        }
        model.addAttribute("orders", orders);
        model.addAttribute("currentStatus", status);
        model.addAttribute("pageTitle", "order");
        return "admin/order/list";
    }

    @PostMapping("/update-status/{id}")
    public String updateStatus(@PathVariable Long id, @RequestParam OrderStatus status, RedirectAttributes ra) {
        try {
            orderService.updateOrderStatus(id, status);
            ra.addFlashAttribute("message", "Cập nhật trạng thái thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/orders";
    }

    @GetMapping("/api/detail/{id}")
    @ResponseBody
    public OrderResponseDTO getOrderDetailApi(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }
}