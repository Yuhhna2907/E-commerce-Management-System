package com.codegym.smartphonemanagement.service.payment;

import com.codegym.smartphonemanagement.config.payment.VNPAYConfig;
import com.codegym.smartphonemanagement.exception.PaymentException;
import com.codegym.smartphonemanagement.model.dto.payment.VNPayRefundRequest;
import com.codegym.smartphonemanagement.model.dto.payment.VNPayRefundResponse;
import com.codegym.smartphonemanagement.model.dto.payment.VNPayRefundStatusResponse;
import com.codegym.smartphonemanagement.util.payment.RefundRequestParser;
import com.codegym.smartphonemanagement.util.payment.RefundResponseParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP client for VNPay API operations with retry logic
 * Handles refund requests and status checks with automatic retry on transient failures
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VNPayAPIClient {

    private final RestTemplate restTemplate;
    private final VNPAYConfig vnpayConfig;
    private final RefundRequestParser refundRequestParser;
    private final RefundResponseParser refundResponseParser;

    @Value("${vnpay.apiUrl:https://sandbox-paymentv2.vnpay.vn}")
    private String vnpayApiUrl;

    @Value("${vnpay.refundUrl:${vnpay.apiUrl}}")
    private String vnpayRefundUrl;

    /**
     * Submit refund request to VNPay API with retry logic
     * Retries on transient network errors with exponential backoff
     * 
     * @param request VNPay refund request
     * @return VNPay refund response
     * @throws PaymentException if all retry attempts fail
     */
    @Retryable(
        retryFor = {SocketTimeoutException.class, ConnectException.class, HttpServerErrorException.class},
        maxAttemptsExpression = "${app.payment.retry.max-attempts:3}",
        backoff = @Backoff(
            delayExpression = "${app.payment.retry.initial-delay:1000}",
            multiplierExpression = "${app.payment.retry.multiplier:2.0}",
            maxDelayExpression = "${app.payment.retry.max-delay:10000}"
        )
    )
    public VNPayRefundResponse submitRefund(VNPayRefundRequest request) {
        log.info("Submitting refund request to VNPay API: requestId={}, amount={}", 
                request.getVnp_RequestId(), request.getVnp_Amount());
        
        try {
            // Create HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-www-form-urlencoded");
            
            // Convert request to URL-encoded form data
            String formData = buildRefundFormData(request);
            
            HttpEntity<String> entity = new HttpEntity<>(formData, headers);
            
            // Make POST request to VNPay refund API
            ResponseEntity<String> response = restTemplate.exchange(
                vnpayRefundUrl + "/merchant_webapi/api/transaction",
                HttpMethod.POST,
                entity,
                String.class
            );
            
            log.info("Received refund response from VNPay: requestId={}, statusCode={}", 
                    request.getVnp_RequestId(), response.getStatusCode());
            
            // Parse response
            return parseRefundResponse(response.getBody());
            
        } catch (HttpServerErrorException e) {
            log.warn("Transient error during refund API call: requestId={}, error={}", 
                    request.getVnp_RequestId(), e.getMessage());
            throw e; // Will trigger retry
        } catch (Exception e) {
            log.error("Non-retryable error during refund API call: requestId={}, error={}", 
                    request.getVnp_RequestId(), e.getMessage(), e);
            throw new PaymentException("Refund API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Check refund status from VNPay API with retry logic
     * 
     * @param refundRequestId Refund request ID to check
     * @return VNPay refund status response
     * @throws PaymentException if all retry attempts fail
     */
    @Retryable(
        retryFor = {SocketTimeoutException.class, ConnectException.class, HttpServerErrorException.class},
        maxAttemptsExpression = "${app.payment.retry.max-attempts:3}",
        backoff = @Backoff(
            delayExpression = "${app.payment.retry.initial-delay:1000}",
            multiplierExpression = "${app.payment.retry.multiplier:2.0}",
            maxDelayExpression = "${app.payment.retry.max-delay:10000}"
        )
    )
    public VNPayRefundStatusResponse checkRefundStatus(String refundRequestId) {
        log.info("Checking refund status from VNPay API: requestId={}", refundRequestId);
        
        try {
            // Create query parameters for status check
            String queryParams = buildStatusCheckParams(refundRequestId);
            
            // Make GET request to VNPay status API
            ResponseEntity<String> response = restTemplate.getForEntity(
                vnpayApiUrl + "/merchant_webapi/api/transaction?" + queryParams,
                String.class
            );
            
            log.info("Received status response from VNPay: requestId={}, statusCode={}", 
                    refundRequestId, response.getStatusCode());
            
            // Parse response
            return parseStatusResponse(response.getBody());
            
        } catch (HttpServerErrorException e) {
            log.warn("Transient error during status check API call: requestId={}, error={}", 
                    refundRequestId, e.getMessage());
            throw e; // Will trigger retry
        } catch (Exception e) {
            log.error("Non-retryable error during status check API call: requestId={}, error={}", 
                    refundRequestId, e.getMessage(), e);
            throw new PaymentException("Status check API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Recovery method called when all retry attempts are exhausted for refund
     * 
     * @param e The exception that caused all retries to fail
     * @param request The original refund request
     * @return Never returns, always throws PaymentException
     * @throws PaymentException Always thrown with details of all failed attempts
     */
    @Recover
    public VNPayRefundResponse recoverFromRefundFailure(Exception e, VNPayRefundRequest request) {
        String errorMessage = String.format(
            "Refund API call failed after all retry attempts: requestId=%s, finalError=%s",
            request.getVnp_RequestId(), e.getMessage()
        );
        
        log.error(errorMessage, e);
        throw new PaymentException(errorMessage, e);
    }

    /**
     * Recovery method called when all retry attempts are exhausted for status check
     * 
     * @param e The exception that caused all retries to fail
     * @param refundRequestId The refund request ID being checked
     * @return Never returns, always throws PaymentException
     * @throws PaymentException Always thrown with details of all failed attempts
     */
    @Recover
    public VNPayRefundStatusResponse recoverFromStatusCheckFailure(Exception e, String refundRequestId) {
        String errorMessage = String.format(
            "Status check API call failed after all retry attempts: requestId=%s, finalError=%s",
            refundRequestId, e.getMessage()
        );
        
        log.error(errorMessage, e);
        throw new PaymentException(errorMessage, e);
    }

    /**
     * Build URL-encoded form data for refund request
     */
    private String buildRefundFormData(VNPayRefundRequest request) {
        // Use RefundRequestParser to generate properly formatted query string
        return refundRequestParser.toQueryString(request);
    }

    /**
     * Build query parameters for status check request
     */
    private String buildStatusCheckParams(String refundRequestId) {
        StringBuilder params = new StringBuilder();
        
        appendParam(params, "vnp_RequestId", refundRequestId);
        appendParam(params, "vnp_Version", "2.1.0");
        appendParam(params, "vnp_Command", "querydr");
        appendParam(params, "vnp_TmnCode", vnpayConfig.getVnp_TmnCode());
        
        // Generate secure hash for status check
        String hashData = "vnp_Command=querydr&vnp_RequestId=" + refundRequestId + 
                         "&vnp_TmnCode=" + vnpayConfig.getVnp_TmnCode() + "&vnp_Version=2.1.0";
        String secureHash = VNPAYConfig.hmacSHA512(vnpayConfig.getVnp_HashSecret(), hashData);
        appendParam(params, "vnp_SecureHash", secureHash);
        
        return params.toString();
    }

    /**
     * Helper method to append URL-encoded parameters
     */
    private void appendParam(StringBuilder sb, String key, String value) {
        if (value != null && !value.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append("&");
            }
            sb.append(key).append("=").append(value);
        }
    }

    /**
     * Parse VNPay refund response from string format
     */
    private VNPayRefundResponse parseRefundResponse(String responseBody) {
        log.debug("Parsing refund response: {}", responseBody);
        
        try {
            // Use RefundResponseParser to parse and validate response
            return refundResponseParser.fromQueryString(responseBody);
        } catch (Exception e) {
            log.error("Failed to parse refund response: {}", e.getMessage(), e);
            throw new PaymentException("Failed to parse refund response: " + e.getMessage(), e);
        }
    }

    /**
     * Parse VNPay status response from string format
     */
    private VNPayRefundStatusResponse parseStatusResponse(String responseBody) {
        log.debug("Parsing status response: {}", responseBody);
        
        try {
            // Parse response body to parameter map
            Map<String, String> params = parseResponseToMap(responseBody);
            
            // Create status response object
            VNPayRefundStatusResponse response = new VNPayRefundStatusResponse();
            response.setVnp_ResponseCode(params.get("vnp_ResponseCode"));
            response.setVnp_Message(params.get("vnp_Message"));
            response.setVnp_RequestId(params.get("vnp_RequestId"));
            response.setVnp_TransactionNo(params.get("vnp_TransactionNo"));
            
            String amountStr = params.get("vnp_Amount");
            if (amountStr != null && !amountStr.isEmpty()) {
                response.setVnp_Amount(new java.math.BigDecimal(amountStr));
            }
            
            response.setVnp_BankCode(params.get("vnp_BankCode"));
            response.setVnp_PayDate(params.get("vnp_PayDate"));
            response.setVnp_SecureHash(params.get("vnp_SecureHash"));
            
            return response;
        } catch (Exception e) {
            log.error("Failed to parse status response: {}", e.getMessage(), e);
            throw new PaymentException("Failed to parse status response: " + e.getMessage(), e);
        }
    }

    /**
     * Parse URL-encoded response body to parameter map
     */
    private Map<String, String> parseResponseToMap(String responseBody) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = responseBody.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                try {
                    String key = java.net.URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                    String value = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                    params.put(key, value);
                } catch (Exception e) {
                    log.error("Failed to decode parameter: {}", pair, e);
                }
            }
        }
        return params;
    }
}