package com.codegym.smartphonemanagement.controller.payment;

import com.codegym.smartphonemanagement.config.payment.VNPAYConfig;
import com.codegym.smartphonemanagement.model.dto.payment.WebhookResponse;
import com.codegym.smartphonemanagement.service.payment.IPNWebhookHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for handling VNPay webhook notifications
 * Provides endpoint for VNPay IPN (Instant Payment Notification) callbacks
 */
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final IPNWebhookHandler webhookHandler;

    /**
     * Handle VNPay IPN webhook notifications
     * Processes payment status updates asynchronously and returns immediate response to VNPay
     * 
     * @param ipnParams Map of IPN parameters from VNPay
     * @param request HttpServletRequest for extracting source IP
     * @return ResponseEntity with webhook response (always HTTP 200 to prevent VNPay retries)
     */
    @PostMapping("/vnpay-ipn")
    public ResponseEntity<WebhookResponse> handleVNPayIPN(
            @RequestParam Map<String, String> ipnParams,
            HttpServletRequest request) {
        
        // Extract source IP for logging and security
        String sourceIp = VNPAYConfig.getIpAddress(request);
        String vnpayTransactionNo = ipnParams.get("vnp_TransactionNo");
        String orderId = ipnParams.get("vnp_TxnRef");
        
        log.info("Received VNPay IPN webhook: orderId={}, vnpayTransactionNo={}, sourceIp={}", 
                orderId, vnpayTransactionNo, sourceIp);

        try {
            // Process webhook asynchronously
            CompletableFuture<WebhookResponse> futureResponse = 
                webhookHandler.processIPN(ipnParams, sourceIp);
            
            // Wait for processing to complete (with timeout)
            WebhookResponse response = futureResponse.get(30, java.util.concurrent.TimeUnit.SECONDS);
            
            log.info("VNPay IPN processing completed: orderId={}, vnpayTransactionNo={}, responseCode={}", 
                    orderId, vnpayTransactionNo, response.getRspCode());
            
            // Always return HTTP 200 to prevent VNPay from retrying
            // Use RspCode in response body to indicate success/failure
            return ResponseEntity.ok(response);
            
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("VNPay IPN processing timeout: orderId={}, vnpayTransactionNo={}", 
                     orderId, vnpayTransactionNo, e);
            
            // Return failure response but still HTTP 200
            return ResponseEntity.ok(WebhookResponse.failure("Processing timeout"));
            
        } catch (Exception e) {
            log.error("VNPay IPN processing error: orderId={}, vnpayTransactionNo={}, error={}", 
                     orderId, vnpayTransactionNo, e.getMessage(), e);
            
            // Return failure response but still HTTP 200
            return ResponseEntity.ok(WebhookResponse.failure("Processing error: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint for webhook service
     * Can be used by VNPay or monitoring systems to verify webhook endpoint availability
     * 
     * @return ResponseEntity with health status
     */
    @GetMapping("/webhook/health")
    public ResponseEntity<Map<String, String>> webhookHealth() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "vnpay-webhook",
            "timestamp", java.time.Instant.now().toString()
        ));
    }
}