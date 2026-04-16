package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.model.Order;
import com.codegym.smartphonemanagement.model.PaymentStatus;
import com.codegym.smartphonemanagement.repository.user.OrderRepository;
import com.codegym.smartphonemanagement.service.payment.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/user/api/payment")
@RequiredArgsConstructor
public class VNPayCallbackController {

    private final VNPayService vnPayService;
    private final OrderRepository orderRepository;

    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request, Model model) {
        Map<String, String[]> requestParameterMap = request.getParameterMap();
        Map<String, String> fields = new HashMap<>();
        for (Map.Entry<String, String[]> entry : requestParameterMap.entrySet()) {
            fields.put(entry.getKey(), entry.getValue()[0]);
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");

        // Xác thực chữ ký
        boolean signVerified = vnPayService.verifySignature(fields);
        
        if (signVerified) {
            String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
            String orderIdStr = request.getParameter("vnp_TxnRef");
            
            try {
                Long orderId = Long.parseLong(orderIdStr);
                Order order = orderRepository.findById(orderId).orElse(null);
                
                if (order != null) {
                    if ("00".equals(vnp_ResponseCode)) {
                        // Thanh toán thành công
                        order.setPaymentStatus(PaymentStatus.COMPLETED);
                        orderRepository.save(order);
                        model.addAttribute("success", true);
                        model.addAttribute("orderId", orderId);
                    } else {
                        // Thanh toán thất bại hoặc hủy (người dùng click Hủy thanh toán)
                        order.setPaymentStatus(PaymentStatus.FAILED);
                        orderRepository.save(order);
                        model.addAttribute("success", false);
                        model.addAttribute("message", "Thanh toán không thành công. Mã lỗi: " + vnp_ResponseCode);
                        model.addAttribute("orderId", orderId);
                    }
                } else {
                    model.addAttribute("success", false);
                    model.addAttribute("message", "Không tìm thấy đơn hàng.");
                }
            } catch (Exception e) {
                model.addAttribute("success", false);
                model.addAttribute("message", "Dữ liệu trả về không hợp lệ.");
            }
        } else {
            model.addAttribute("success", false);
            model.addAttribute("message", "Xác thực chữ ký thất bại. Dữ liệu có thể đã bị can thiệp!");
        }

        return "user/order/payment-result";
    }
}
