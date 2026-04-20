package com.codegym.smartphonemanagement.util.payment;

import com.codegym.smartphonemanagement.config.payment.VNPAYConfig;
import com.codegym.smartphonemanagement.exception.PaymentException;
import com.codegym.smartphonemanagement.model.dto.payment.VNPayRefundRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility class for parsing and formatting VNPay refund requests
 * Handles serialization to URL-encoded query strings and signature generation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefundRequestParser {

    private final VNPAYConfig vnpayConfig;

    /**
     * Parse VNPayRefundRequest to URL-encoded query string
     * Validates all required fields and generates HMAC-SHA512 signature
     *
     * @param request the refund request object
     * @return URL-encoded query string with signature
     * @throws PaymentException if validation fails or required fields are missing
     */
    public String toQueryString(VNPayRefundRequest request) {
        try {
            // Validate required fields
            validateRefundRequest(request);

            // Build parameter map (sorted for consistent signature)
            Map<String, String> params = new TreeMap<>();
            params.put("vnp_RequestId", request.getVnp_RequestId());
            params.put("vnp_Version", request.getVnp_Version());
            params.put("vnp_Command", request.getVnp_Command());
            params.put("vnp_TmnCode", request.getVnp_TmnCode());
            params.put("vnp_TransactionType", request.getVnp_TransactionType());
            params.put("vnp_TxnRef", request.getVnp_TxnRef());
            params.put("vnp_Amount", String.valueOf(request.getVnp_Amount().longValue()));
            params.put("vnp_OrderInfo", request.getVnp_OrderInfo());
            params.put("vnp_TransactionNo", request.getVnp_TransactionNo());
            params.put("vnp_TransactionDate", request.getVnp_TransactionDate());
            params.put("vnp_CreateBy", request.getVnp_CreateBy());
            params.put("vnp_CreateDate", request.getVnp_CreateDate());
            params.put("vnp_IpAddr", request.getVnp_IpAddr());

            // Generate signature
            String hashData = buildHashData(params);
            String secureHash = VNPAYConfig.hmacSHA512(vnpayConfig.getVnp_HashSecret(), hashData);
            params.put("vnp_SecureHash", secureHash);

            // Build query string
            return buildQueryString(params);

        } catch (Exception e) {
            log.error("Failed to parse refund request to query string: {}", e.getMessage(), e);
            throw new PaymentException("Failed to parse refund request: " + e.getMessage(), e);
        }
    }

    /**
     * Parse VNPayRefundRequest from query string
     * Used for round-trip testing
     *
     * @param queryString the URL-encoded query string
     * @return VNPayRefundRequest object
     */
    public VNPayRefundRequest fromQueryString(String queryString) {
        try {
            Map<String, String> params = parseQueryString(queryString);

            return VNPayRefundRequest.builder()
                    .vnp_RequestId(params.get("vnp_RequestId"))
                    .vnp_Version(params.get("vnp_Version"))
                    .vnp_Command(params.get("vnp_Command"))
                    .vnp_TmnCode(params.get("vnp_TmnCode"))
                    .vnp_TransactionType(params.get("vnp_TransactionType"))
                    .vnp_TxnRef(params.get("vnp_TxnRef"))
                    .vnp_Amount(new java.math.BigDecimal(params.get("vnp_Amount")))
                    .vnp_OrderInfo(params.get("vnp_OrderInfo"))
                    .vnp_TransactionNo(params.get("vnp_TransactionNo"))
                    .vnp_TransactionDate(params.get("vnp_TransactionDate"))
                    .vnp_CreateBy(params.get("vnp_CreateBy"))
                    .vnp_CreateDate(params.get("vnp_CreateDate"))
                    .vnp_IpAddr(params.get("vnp_IpAddr"))
                    .vnp_SecureHash(params.get("vnp_SecureHash"))
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse refund request from query string: {}", e.getMessage(), e);
            throw new PaymentException("Failed to parse refund request: " + e.getMessage(), e);
        }
    }

    /**
     * Validate all required fields in refund request
     */
    private void validateRefundRequest(VNPayRefundRequest request) {
        if (request.getVnp_RequestId() == null || request.getVnp_RequestId().isEmpty()) {
            throw new PaymentException("vnp_RequestId is required");
        }
        if (request.getVnp_Version() == null || request.getVnp_Version().isEmpty()) {
            throw new PaymentException("vnp_Version is required");
        }
        if (request.getVnp_Command() == null || request.getVnp_Command().isEmpty()) {
            throw new PaymentException("vnp_Command is required");
        }
        if (request.getVnp_TmnCode() == null || request.getVnp_TmnCode().isEmpty()) {
            throw new PaymentException("vnp_TmnCode is required");
        }
        if (request.getVnp_TransactionType() == null || request.getVnp_TransactionType().isEmpty()) {
            throw new PaymentException("vnp_TransactionType is required");
        }
        if (request.getVnp_TxnRef() == null || request.getVnp_TxnRef().isEmpty()) {
            throw new PaymentException("vnp_TxnRef is required");
        }
        if (request.getVnp_Amount() == null) {
            throw new PaymentException("vnp_Amount is required");
        }
        if (request.getVnp_OrderInfo() == null || request.getVnp_OrderInfo().isEmpty()) {
            throw new PaymentException("vnp_OrderInfo is required");
        }
        if (request.getVnp_TransactionNo() == null || request.getVnp_TransactionNo().isEmpty()) {
            throw new PaymentException("vnp_TransactionNo is required");
        }
        if (request.getVnp_TransactionDate() == null || request.getVnp_TransactionDate().isEmpty()) {
            throw new PaymentException("vnp_TransactionDate is required");
        }
        if (request.getVnp_CreateBy() == null || request.getVnp_CreateBy().isEmpty()) {
            throw new PaymentException("vnp_CreateBy is required");
        }
        if (request.getVnp_CreateDate() == null || request.getVnp_CreateDate().isEmpty()) {
            throw new PaymentException("vnp_CreateDate is required");
        }
        if (request.getVnp_IpAddr() == null || request.getVnp_IpAddr().isEmpty()) {
            throw new PaymentException("vnp_IpAddr is required");
        }
    }

    /**
     * Build hash data from parameters (sorted, excluding vnp_SecureHash)
     */
    private String buildHashData(Map<String, String> params) {
        StringBuilder hashData = new StringBuilder();
        params.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("vnp_SecureHash"))
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
