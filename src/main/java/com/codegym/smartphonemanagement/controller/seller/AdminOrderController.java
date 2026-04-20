package com.codegym.smartphonemanagement.controller.seller;

import com.codegym.smartphonemanagement.model.Order;
import com.codegym.smartphonemanagement.model.OrderStatus;
import com.codegym.smartphonemanagement.model.RefundStatus;
import com.codegym.smartphonemanagement.repository.user.OrderRepository;
import com.codegym.smartphonemanagement.service.order.RefundService;
import com.codegym.smartphonemanagement.service.order.user.OrderService;
import com.codegym.smartphonemanagement.service.order.DTO.OrderResponseDTO;
import com.codegym.smartphonemanagement.service.order.DTO.RefundResponseDTO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;
    private final RefundService refundService;
    private final OrderRepository orderRepository;

    private static final int PAGE_SIZE = 10;

    // ======================== 1. LIST with PAGINATION + SEARCH + DATE RANGE + STATUS FILTER ========================

    @GetMapping
    public String listOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        // Convert dates
        LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : null;
        LocalDateTime to = dateTo != null ? dateTo.atTime(LocalTime.MAX) : null;

        // Paginated search
        Page<Order> orderPage = orderRepository.searchOrders(
                status, keyword, from, to, PageRequest.of(page, PAGE_SIZE));

        // Map to DTO
        Page<OrderResponseDTO> dtoPage = orderPage.map(o -> orderService.getOrderById(o.getId()));

        // Status badge counts (always from full dataset, not filtered)
        long countPending = orderRepository.countByStatus(OrderStatus.PENDING);
        long countConfirmed = orderRepository.countByStatus(OrderStatus.CONFIRMED);
        long countShipping = orderRepository.countByStatus(OrderStatus.SHIPPING);
        long countDelivered = orderRepository.countByStatus(OrderStatus.DELIVERED);
        long countCancelled = orderRepository.countByStatus(OrderStatus.CANCELLED);
        long countRefundRequested = orderRepository.countByStatus(OrderStatus.REFUND_REQUESTED);
        long countAll = countPending + countConfirmed + countShipping + countDelivered + countCancelled + countRefundRequested
                + orderRepository.countByStatus(OrderStatus.REFUNDED)
                + orderRepository.countByStatus(OrderStatus.PARTIAL_REFUNDED);

        model.addAttribute("orders", dtoPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", dtoPage.getTotalPages());
        model.addAttribute("totalElements", dtoPage.getTotalElements());
        model.addAttribute("currentStatus", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("pageTitle", "order");

        // KPI counts
        model.addAttribute("countAll", countAll);
        model.addAttribute("countPending", countPending);
        model.addAttribute("countConfirmed", countConfirmed);
        model.addAttribute("countShipping", countShipping);
        model.addAttribute("countDelivered", countDelivered);
        model.addAttribute("countCancelled", countCancelled);
        model.addAttribute("countRefundRequested", countRefundRequested);

        // Sidebar badge
        model.addAttribute("countRefundPending",
                refundService.getRefundsByStatus(RefundStatus.PENDING).size());

        return "admin/order/list";
    }

    // ======================== 2. CHI TIẾT ========================

    @GetMapping("/detail/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        OrderResponseDTO order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        model.addAttribute("pageTitle", "order");
        return "admin/order/detail";
    }

    // ======================== 3. AJAX STATUS UPDATE ========================

    @PostMapping("/api/{id}/confirm")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> confirmOrderAjax(@PathVariable Long id) {
        try {
            orderService.updateOrderStatus(id, OrderStatus.CONFIRMED, "Admin xác nhận đơn hàng");
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã xác nhận đơn #" + id, "newStatus", "CONFIRMED", "newStatusDisplay", "Đã xác nhận"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/{id}/ship")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> shipOrderAjax(@PathVariable Long id) {
        try {
            orderService.updateOrderStatus(id, OrderStatus.SHIPPING, "Bắt đầu vận chuyển");
            return ResponseEntity.ok(Map.of("success", true, "message", "Đơn #" + id + " đang giao", "newStatus", "SHIPPING", "newStatusDisplay", "Đang vận chuyển"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/{id}/deliver")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deliverOrderAjax(@PathVariable Long id) {
        try {
            orderService.updateOrderStatus(id, OrderStatus.DELIVERED, "Giao hàng thành công");
            return ResponseEntity.ok(Map.of("success", true, "message", "Đơn #" + id + " đã giao", "newStatus", "DELIVERED", "newStatusDisplay", "Đã giao hàng"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/{id}/cancel")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelOrderAjax(@PathVariable Long id) {
        try {
            orderService.updateOrderStatus(id, OrderStatus.CANCELLED, "Admin hủy đơn hàng");
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã hủy đơn #" + id, "newStatus", "CANCELLED", "newStatusDisplay", "Đã hủy"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ======================== 4. LEGACY FORM SUBMISSIONS (fallback) ========================

    @PostMapping("/{id}/confirm")
    public String confirmOrder(@PathVariable Long id, RedirectAttributes ra) {
        try {
            orderService.updateOrderStatus(id, OrderStatus.CONFIRMED, "Admin xác nhận đơn hàng");
            ra.addFlashAttribute("message", "Đã xác nhận đơn hàng #" + id);
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/orders";
    }

    @PostMapping("/{id}/ship")
    public String shipOrder(@PathVariable Long id, RedirectAttributes ra) {
        try {
            orderService.updateOrderStatus(id, OrderStatus.SHIPPING, "Bắt đầu vận chuyển");
            ra.addFlashAttribute("message", "Đơn #" + id + " đang được vận chuyển");
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/orders";
    }

    @PostMapping("/{id}/deliver")
    public String deliverOrder(@PathVariable Long id, RedirectAttributes ra) {
        try {
            orderService.updateOrderStatus(id, OrderStatus.DELIVERED, "Giao hàng thành công");
            ra.addFlashAttribute("message", "Đơn #" + id + " đã giao thành công");
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/orders";
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id, @RequestParam(required = false) String reason, RedirectAttributes ra) {
        try {
            orderService.updateOrderStatus(id, OrderStatus.CANCELLED, reason != null ? reason : "Admin hủy đơn hàng");
            ra.addFlashAttribute("message", "Đã hủy đơn hàng #" + id + " (đã hoàn kho + voucher)");
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/orders";
    }

    @PostMapping("/update-status/{id}")
    public String updateStatus(@PathVariable Long id, @RequestParam OrderStatus status, RedirectAttributes ra) {
        try {
            orderService.updateOrderStatus(id, status);
            ra.addFlashAttribute("message", "Cập nhật trạng thái thành công!");
        } catch (Exception e) { ra.addFlashAttribute("error", "Lỗi: " + e.getMessage()); }
        return "redirect:/admin/orders";
    }

    // ======================== 5. EXPORT CSV ========================

    @GetMapping("/export")
    public void exportCSV(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=orders_export_" + LocalDate.now() + ".csv");

        LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : null;
        LocalDateTime to = dateTo != null ? dateTo.atTime(LocalTime.MAX) : null;

        // Get ALL matching results (no pagination for export)
        Page<Order> allOrders = orderRepository.searchOrders(status, keyword, from, to, PageRequest.of(0, 10000));

        PrintWriter writer = response.getWriter();
        // BOM for Excel UTF-8
        writer.write('\uFEFF');
        writer.println("Mã đơn,Khách hàng,SĐT,Tổng tiền,Trạng thái,Ngày đặt,Địa chỉ,Ghi chú");

        for (Order o : allOrders.getContent()) {
            String line = String.format("\"%d\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                    o.getId(),
                    escape(o.getReceiverName()),
                    escape(o.getReceiverPhone()),
                    o.getTotalPrice() != null ? o.getTotalPrice().toPlainString() : "0",
                    o.getStatus() != null ? o.getStatus().name() : "",
                    o.getCreatedAt() != null ? o.getCreatedAt().toString() : "",
                    escape(o.getShippingAddress()),
                    escape(o.getNote()));
            writer.println(line);
        }
        writer.flush();
    }

    private String escape(String val) {
        if (val == null) return "";
        return val.replace("\"", "\"\"");
    }

    // ======================== 6. REFUND MANAGEMENT (giữ nguyên) ========================

    @GetMapping("/refunds")
    public String listRefunds(@RequestParam(required = false) RefundStatus status, Model model) {
        List<RefundResponseDTO> refunds;
        if (status != null) { refunds = refundService.getRefundsByStatus(status); }
        else { refunds = refundService.getAllRefundRequests(); }
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
    public String approveRefund(@PathVariable Long id, @RequestParam(required = false) String adminNote, RedirectAttributes ra) {
        try {
            refundService.approveRefund(id, adminNote);
            ra.addFlashAttribute("message", "Đã duyệt hoàn trả thành công!");
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/orders/refunds";
    }

    @PostMapping("/refunds/{id}/reject")
    public String rejectRefund(@PathVariable Long id, @RequestParam(required = false) String adminNote, RedirectAttributes ra) {
        try {
            refundService.rejectRefund(id, adminNote);
            ra.addFlashAttribute("message", "Đã từ chối yêu cầu hoàn trả.");
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/orders/refunds";
    }

    @GetMapping("/api/detail/{id}")
    @ResponseBody
    public OrderResponseDTO getOrderDetailApi(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }
}