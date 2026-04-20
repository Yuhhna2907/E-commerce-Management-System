package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.model.Order;
import com.codegym.smartphonemanagement.model.PaymentStatus;
import com.codegym.smartphonemanagement.repository.user.OrderRepository;
import com.codegym.smartphonemanagement.service.payment.PaymentTransactionLogger;
import com.codegym.smartphonemanagement.service.payment.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/user/api/payment")
@RequiredArgsConstructor
@Slf4j
public class VNPayCallbackController {

    private final VNPayService vnPayService;
    private final OrderRepository orderRepository;
    private final PaymentTransactionLogger transactionLogger;

    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request, Model model) {
        Map<String, String[]> requestParameterMap = request.getParameterMap();
        Map<String, String> fields = new HashMap<>();
        for (Map.Entry<String, String[]> entry : requestParameterMap.entrySet()) {
            fields.put(entry.getKey(), entry.getValue()[0]);
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        String vnp_TransactionNo = request.getParameter("vnp_TransactionNo");
        String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
        String orderIdStr = request.getParameter("vnp_TxnRef");

        log.info("VNPay callback received for order: {}, transaction: {}, response code: {}", 
                orderIdStr, vnp_TransactionNo, vnp_ResponseCode);

        // Xác thực chữ ký
        boolean signVerified = vnPayService.verifySignature(fields);
        
        if (signVerified) {
            try {
                Long orderId = Long.parseLong(orderIdStr);
                Order order = orderRepository.findById(orderId).orElse(null);
                
                if (order != null) {
                    // Update payment transaction from VNPay callback
                    try {
                        transactionLogger.updateFromCallback(vnp_TransactionNo, fields);
                        log.info("Successfully updated transaction for order ID: {}", orderId);
                    } catch (Exception loggingError) {
                        log.error("Failed to update transaction for order ID: {}, continuing with order update", 
                                orderId, loggingError);
                    }
                    
                    if ("00".equals(vnp_ResponseCode)) {
                        // Thanh toán thành công
                        order.setPaymentStatus(PaymentStatus.COMPLETED);
                        orderRepository.save(order);
                        model.addAttribute("success", true);
                        model.addAttribute("orderId", orderId);
                        log.info("Payment completed successfully for order ID: {}", orderId);
                    } else {
                        // Thanh toán thất bại hoặc hủy (người dùng click Hủy thanh toán)
                        order.setPaymentStatus(PaymentStatus.FAILED);
                        orderRepository.save(order);
                        model.addAttribute("success", false);
                        model.addAttribute("message", "Thanh toán không thành công. Mã lỗi: " + vnp_ResponseCode);
                        model.addAttribute("orderId", orderId);
                        log.warn("Payment failed for order ID: {}, response code: {}", orderId, vnp_ResponseCode);
                    }
                } else {
                    model.addAttribute("success", false);
                    model.addAttribute("message", "Không tìm thấy đơn hàng.");
                    log.error("Order not found for ID: {}", orderId);
                }
            } catch (Exception e) {
                model.addAttribute("success", false);
                model.addAttribute("message", "Dữ liệu trả về không hợp lệ.");
                log.error("Error processing VNPay callback", e);
            }
        } else {
            model.addAttribute("success", false);
            model.addAttribute("message", "Xác thực chữ ký thất bại. Dữ liệu có thể đã bị can thiệp!");
            log.error("VNPay signature verification failed for order: {}", orderIdStr);
        }

        return "user/order/payment-result";
    }
}
