package com.codegym.smartphonemanagement.util.payment;

import com.codegym.smartphonemanagement.config.payment.VNPAYConfig;
import com.codegym.smartphonemanagement.exception.PaymentException;
import com.codegym.smartphonemanagement.model.RefundStatus;
import com.codegym.smartphonemanagement.model.dto.payment.VNPayRefundResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility class for parsing and formatting VNPay refund responses
 * Handles deserialization from VNPay format and signature validation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefundResponseParser {

    private final VNPAYConfig vnpayConfig;

    /**
     * Parse VNPay refund response from parameter map
     * Validates response signature before parsing
     *
     * @param params the response parameters from VNPay
     * @return VNPayRefundResponse object
     * @throws PaymentException if signature validation fails
     */
    public VNPayRefundResponse fromParams(Map<String, String> params) {
        try {
            // Validate signature
            String providedSignature = params.get("vnp_SecureHash");
            if (providedSignature == null || providedSignature.isEmpty()) {
                throw new PaymentException("Missing vnp_SecureHash in response");
            }

            // Build hash data (exclude vnp_SecureHash)
            String hashData = buildHashData(params);
            String calculatedSignature = VNPAYConfig.hmacSHA512(vnpayConfig.getVnp_HashSecret(), hashData);

            if (!calculatedSignature.equals(providedSignature)) {
                log.error("Invalid refund response signature. Expected: {}, Got: {}", calculatedSignature, providedSignature);
                throw new PaymentException("Invalid refund response signature - possible security breach");
            }

            // Parse response
            VNPayRefundResponse response = new VNPayRefundResponse();
            response.setVnp_ResponseCode(params.get("vnp_ResponseCode"));
            response.setVnp_Message(params.get("vnp_Message"));
            response.setVnp_TransactionNo(params.get("vnp_TransactionNo"));
            
            String amountStr = params.get("vnp_Amount");
            if (amountStr != null && !amountStr.isEmpty()) {
                response.setVnp_Amount(new BigDecimal(amountStr));
            }
            
            response.setVnp_BankCode(params.get("vnp_BankCode"));
            response.setVnp_PayDate(params.get("vnp_PayDate"));
            response.setVnp_SecureHash(providedSignature);

            return response;

        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse refund response: {}", e.getMessage(), e);
            throw new PaymentException("Failed to parse refund response: " + e.getMessage(), e);
        }
    }

    /**
     * Parse VNPay refund response from query string
     *
     * @param queryString the URL-encoded query string
     * @return VNPayRefundResponse object
     * @throws PaymentException if signature validation fails
     */
    public VNPayRefundResponse fromQueryString(String queryString) {
        Map<String, String> params = parseQueryString(queryString);
        return fromParams(params);
    }

    /**
     * Convert VNPayRefundResponse to query string format
     * Used for round-trip testing
     *
     * @param response the refund response object
     * @return URL-encoded query string with signature
     */
    public String toQueryString(VNPayRefundResponse response) {
        try {
            Map<String, String> params = new TreeMap<>();
            
            if (response.getVnp_ResponseCode() != null) {
                params.put("vnp_ResponseCode", response.getVnp_ResponseCode());
            }
            if (response.getVnp_Message() != null) {
                params.put("vnp_Message", response.getVnp_Message());
            }
            if (response.getVnp_TransactionNo() != null) {
                params.put("vnp_TransactionNo", response.getVnp_TransactionNo());
            }
            if (response.getVnp_Amount() != null) {
                params.put("vnp_Amount", String.valueOf(response.getVnp_Amount().longValue()));
            }
            if (response.getVnp_BankCode() != null) {
                params.put("vnp_BankCode", response.getVnp_BankCode());
            }
            if (response.getVnp_PayDate() != null) {
                params.put("vnp_PayDate", response.getVnp_PayDate());
            }

            // Generate signature
            String hashData = buildHashData(params);
            String secureHash = VNPAYConfig.hmacSHA512(vnpayConfig.getVnp_HashSecret(), hashData);
            params.put("vnp_SecureHash", secureHash);

            return buildQueryString(params);

        } catch (Exception e) {
            log.error("Failed to convert refund response to query string: {}", e.getMessage(), e);
            throw new PaymentException("Failed to convert refund response: " + e.getMessage(), e);
        }
    }

    /**
     * Map VNPay response code to RefundStatus
     *
     * @param responseCode the VNPay response code
     * @return corresponding RefundStatus
     */
    public RefundStatus mapResponseCodeToStatus(String responseCode) {
        if (responseCode == null) {
            return RefundStatus.FAILED;
        }

        switch (responseCode) {
            case "00":
                return RefundStatus.COMPLETED;
            case "02":
                return RefundStatus.PROCESSING;
            default:
                return RefundStatus.FAILED;
        }
    }

    /**
     * Build hash data from parameters (sorted, excluding vnp_SecureHash)
     */
    private String buildHashData(Map<String, String> params) {
        StringBuilder hashData = new StringBuilder();
        params.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("vnp_SecureHash"))
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .forEach(entry -> {
                    if (hashData.length() > 0) {
                        hashData.append("&");
                    }
                    hashData.append(entry.getKey()).append("=").append(entry.getValue());
                });
        return hashData.toString();
    }

    /**
     * Build URL-encoded query string from parameters
     */
    private String buildQueryString(Map<String, String> params) throws UnsupportedEncodingException {
        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (queryString.length() > 0) {
                queryString.append("&");
            }
            queryString.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.toString()))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString()));
        }
        return queryString.toString();
    }

    /**
     * Parse query string to parameter map
     */
    private Map<String, String> parseQueryString(String queryString) {
        Map<String, String> params = new TreeMap<>();
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                try {
                    String key = java.net.URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8.toString());
                    String value = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.toString());
                    params.put(key, value);
                } catch (UnsupportedEncodingException e) {
                    log.error("Failed to decode query string parameter: {}", pair, e);
                }
            }
        }
        return params;
    }
}
