package com.codegym.smartphonemanagement.controller.seller;

import com.codegym.smartphonemanagement.model.OrderStatus;
import com.codegym.smartphonemanagement.model.RefundStatus;
import com.codegym.smartphonemanagement.service.order.RefundService;
import com.codegym.smartphonemanagement.service.order.user.OrderService;
import com.codegym.smartphonemanagement.service.order.DTO.OrderResponseDTO;
import com.codegym.smartphonemanagement.service.order.DTO.RefundResponseDTO;
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
    private final RefundService refundService;

    // ======================== LIST / FILTER ========================

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

        // Badge counts cho sidebar
        model.addAttribute("countPending", orderService.getOrdersByStatus(OrderStatus.PENDING).size());
        model.addAttribute("countRefundRequested",
                refundService.getRefundsByStatus(RefundStatus.PENDING).size());

        return "admin/order/list";
    }

    // ======================== CHI TIẾT ========================

    @GetMapping("/detail/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        OrderResponseDTO order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        model.addAttribute("pageTitle", "order");
        return "admin/order/detail";
    }

    // ======================== CHUYỂN TRẠNG THÁI ========================

    @PostMapping("/{id}/confirm")
    public String confirmOrder(@PathVariable Long id, RedirectAttributes ra) {
        try {
            orderService.updateOrderStatus(id, OrderStatus.CONFIRMED, "Admin xác nhận đơn hàng");
            ra.addFlashAttribute("message", "Đã xác nhận đơn hàng #" + id);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/{id}/ship")
    public String shipOrder(@PathVariable Long id, RedirectAttributes ra) {
        try {
            orderService.updateOrderStatus(id, OrderStatus.SHIPPING, "Bắt đầu vận chuyển");
            ra.addFlashAttribute("message", "Đơn #" + id + " đang được vận chuyển");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/{id}/deliver")
    public String deliverOrder(@PathVariable Long id, RedirectAttributes ra) {
        try {
            orderService.updateOrderStatus(id, OrderStatus.DELIVERED, "Giao hàng thành công");
            ra.addFlashAttribute("message", "Đơn #" + id + " đã giao thành công");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @RequestParam(required = false) String reason,
                              RedirectAttributes ra) {
        try {
            orderService.updateOrderStatus(id, OrderStatus.CANCELLED,
                    reason != null ? reason : "Admin hủy đơn hàng");
            ra.addFlashAttribute("message", "Đã hủy đơn hàng #" + id + " (đã hoàn kho + voucher)");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders";
    }

    // Backward compat — old form submission
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

    // ======================== REFUND MANAGEMENT ========================

    @GetMapping("/refunds")
    public String listRefunds(@RequestParam(required = false) RefundStatus status, Model model) {
        List<RefundResponseDTO> refunds;
        if (status != null) {
            refunds = refundService.getRefundsByStatus(status);
        } else {
            refunds = refundService.getAllRefundRequests();
        }
        model.addAttribute("refunds", refunds);
        model.addAttribute("currentStatus", status);
        model.addAttribute("pageTitle", "refund");
        model.addAttribute("countPending", refundService.getRefundsByStatus(RefundStatus.PENDING).size());
        return "admin/refund/list";
    }

    @GetMapping("/refunds/{id}")
    public String refundDetail(@PathVariable Long id, Model model) {
        RefundResponseDTO refund = refundService.getRefundById(id);
        OrderResponseDTO order = orderService.getOrderById(refund.getOrderId());
        model.addAttribute("refund", refund);
        model.addAttribute("order", order);
        model.addAttribute("pageTitle", "refund");
        return "admin/refund/detail";
    }

    @PostMapping("/refunds/{id}/approve")
    public String approveRefund(@PathVariable Long id,
                                @RequestParam(required = false) String adminNote,
                                RedirectAttributes ra) {
        try {
            refundService.approveRefund(id, adminNote);
            ra.addFlashAttribute("message", "Đã duyệt hoàn trả thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/refunds";
    }

    @PostMapping("/refunds/{id}/reject")
    public String rejectRefund(@PathVariable Long id,
                               @RequestParam(required = false) String adminNote,
                               RedirectAttributes ra) {
        try {
            refundService.rejectRefund(id, adminNote);
            ra.addFlashAttribute("message", "Đã từ chối yêu cầu hoàn trả.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/refunds";
    }

    // ======================== API (existing) ========================

    @GetMapping("/api/detail/{id}")
    @ResponseBody
    public OrderResponseDTO getOrderDetailApi(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }
}