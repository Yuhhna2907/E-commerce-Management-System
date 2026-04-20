package com.codegym.smartphonemanagement.service.payment;

import com.codegym.smartphonemanagement.config.payment.VNPAYConfig;
import com.codegym.smartphonemanagement.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for VNPay payment gateway integration
 * Handles payment URL generation, signature verification, and transaction logging
 * 
 * Features:
 * - Comprehensive input validation
 * - Secure signature generation and verification
 * - Transaction logging
 * - Error handling with custom exceptions
 * - Support for payment timeout configuration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayService {

    private final VNPAYConfig vnpayConfig;
    private final PaymentTransactionLogger transactionLogger;
    
    // Constants
    private static final String VNP_VERSION = "2.1.0";
    private static final String VNP_COMMAND = "pay";
    private static final String ORDER_TYPE = "other";
    private static final String CURRENCY_CODE = "VND";
    private static final String LOCALE_VN = "vn";
    private static final String TIMEZONE_GMT7 = "Etc/GMT+7";
    private static final String DATE_FORMAT = "yyyyMMddHHmmss";
    private static final int DEFAULT_PAYMENT_TIMEOUT_MINUTES = 15;
    private static final int AMOUNT_MULTIPLIER = 100; // VNPay requires amount * 100
    
    // Validation constants
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("10000"); // 10,000 VND
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("500000000"); // 500,000,000 VND
    
    /**
     * Create VNPay payment URL with comprehensive validation
     * 
     * @param orderId Order ID for transaction reference
     * @param amount Payment amount in VND
     * @param ipAddress Customer IP address
     * @return Payment URL for redirect
     * @throws PaymentException if validation fails or URL generation fails
     */
    public String createPaymentUrl(Long orderId, BigDecimal amount, String ipAddress) {
        log.info("Creating VNPay payment URL for order ID: {}, amount: {}, IP: {}", orderId, amount, ipAddress);
        
        try {
            // Comprehensive validation
            validatePaymentRequest(orderId, amount, ipAddress);
            
            // Validate and get return URL
            String returnUrl = validateAndGetReturnUrl();
            
            // Prepare payment parameters
            Map<String, String> vnpParams = buildPaymentParams(orderId, amount, ipAddress, returnUrl);
            
            // Generate secure hash and build payment URL
            String paymentUrl = generatePaymentUrl(vnpParams);
            
            // Log INITIATED transaction to database
            try {
                transactionLogger.logInitiated(orderId, amount);
                log.debug("Transaction logged with INITIATED status for order ID: {}", orderId);
            } catch (Exception loggingError) {
                // Log error but don't block payment flow
                log.error("Failed to log INITIATED transaction for order ID: {}, continuing with payment", 
                        orderId, loggingError);
            }
            
            log.info("Successfully created VNPay payment URL for order ID: {}", orderId);
            return paymentUrl;
            
        } catch (PaymentException e) {
            log.error("Payment validation failed for order ID: {}: {}", orderId, e.getMessage());
            // Log error to database
            try {
                transactionLogger.logError(orderId, e.getMessage());
            } catch (Exception loggingError) {
                log.error("Failed to log payment error for order ID: {}", orderId, loggingError);
            }
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating payment URL for order ID: {}", orderId, e);
            // Log error to database
            try {
                transactionLogger.logError(orderId, "Lỗi hệ thống: " + e.getMessage());
            } catch (Exception loggingError) {
                log.error("Failed to log payment error for order ID: {}", orderId, loggingError);
            }
            throw new PaymentException("Lỗi hệ thống khi tạo URL thanh toán. Vui lòng thử lại sau.", e);
        }
    }
    
    /**
     * Verify VNPay payment signature from callback
     * 
     * @param requestParams Parameters from VNPay callback
     * @return true if signature is valid, false otherwise
     */
    public boolean verifySignature(Map<String, String> requestParams) {
        log.info("Verifying VNPay signature for transaction");
        
        try {
            // Validate request params
            if (requestParams == null || requestParams.isEmpty()) {
                log.warn("Empty request params for signature verification");
                return false;
            }
            
            String vnpSecureHash = requestParams.get("vnp_SecureHash");
            if (vnpSecureHash == null || vnpSecureHash.trim().isEmpty()) {
                log.warn("Missing vnp_SecureHash in request params");
                return false;
            }
            
            // Build hash data from params (excluding signature fields)
            Map<String, String> vnpParams = new HashMap<>(requestParams);
            vnpParams.remove("vnp_SecureHashType");
            vnpParams.remove("vnp_SecureHash");
            
            String hashData = buildHashData(vnpParams);
            String calculatedHash = VNPAYConfig.hmacSHA512(vnpayConfig.getVnp_HashSecret(), hashData);
            
            boolean isValid = calculatedHash.equals(vnpSecureHash);
            
            if (isValid) {
                log.info("VNPay signature verification successful");
            } else {
                log.warn("VNPay signature verification failed. Expected: {}, Got: {}", 
                    calculatedHash.substring(0, 10) + "...", 
                    vnpSecureHash.substring(0, 10) + "...");
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Error verifying VNPay signature", e);
            return false;
        }
    }
    
    /**
     * Extract transaction info from VNPay callback params
     * 
     * @param requestParams Parameters from VNPay callback
     * @return Map containing transaction details
     */
    public Map<String, Object> extractTransactionInfo(Map<String, String> requestParams) {
        log.debug("Extracting transaction info from VNPay callback");
        
        Map<String, Object> transactionInfo = new HashMap<>();
        
        try {
            transactionInfo.put("orderId", requestParams.get("vnp_TxnRef"));
            transactionInfo.put("amount", parseAmount(requestParams.get("vnp_Amount")));
            transactionInfo.put("responseCode", requestParams.get("vnp_ResponseCode"));
            transactionInfo.put("transactionNo", requestParams.get("vnp_TransactionNo"));
            transactionInfo.put("bankCode", requestParams.get("vnp_BankCode"));
            transactionInfo.put("payDate", requestParams.get("vnp_PayDate"));
            transactionInfo.put("transactionStatus", requestParams.get("vnp_TransactionStatus"));
            transactionInfo.put("cardType", requestParams.get("vnp_CardType"));
            
            // Determine payment status
            String responseCode = requestParams.get("vnp_ResponseCode");
            boolean isSuccess = "00".equals(responseCode);
            transactionInfo.put("isSuccess", isSuccess);
            transactionInfo.put("message", getResponseMessage(responseCode));
            
            log.info("Extracted transaction info for order: {}, status: {}", 
                transactionInfo.get("orderId"), 
                isSuccess ? "SUCCESS" : "FAILED");
            
        } catch (Exception e) {
            log.error("Error extracting transaction info", e);
            transactionInfo.put("error", "Lỗi khi xử lý thông tin giao dịch");
        }
        
        return transactionInfo;
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    /**
     * Validate payment request parameters
     */
    private void validatePaymentRequest(Long orderId, BigDecimal amount, String ipAddress) {
        // Validate order ID
        if (orderId == null || orderId <= 0) {
            throw new PaymentException("Order ID không hợp lệ");
        }
        
        // Validate amount
        if (amount == null) {
            throw new PaymentException("Số tiền thanh toán không được để trống");
        }
        
        if (amount.compareTo(MIN_AMOUNT) < 0) {
            throw new PaymentException(
                String.format("Số tiền thanh toán tối thiểu là %,d VND", MIN_AMOUNT.longValue())
            );
        }
        
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new PaymentException(
                String.format("Số tiền thanh toán tối đa là %,d VND", MAX_AMOUNT.longValue())
            );
        }
        
        // Validate IP address
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new PaymentException("IP address không hợp lệ");
        }
        
        if ("Invalid IP".equals(ipAddress)) {
            throw new PaymentException("Không thể xác định IP address của khách hàng");
        }
        
        log.debug("Payment request validation passed for order ID: {}", orderId);
    }
    
    /**
     * Validate and get return URL from configuration
     */
    private String validateAndGetReturnUrl() {
        String returnUrl = vnpayConfig.getVnp_ReturnUrl();
        
        if (returnUrl == null || returnUrl.trim().isEmpty()) {
            throw new PaymentException("Return URL không được cấu hình trong hệ thống");
        }
        
        // Validate return URL format for security
        if (!returnUrl.startsWith("http://localhost") && 
            !returnUrl.startsWith("https://localhost") &&
            !returnUrl.startsWith("https://")) {
            throw new PaymentException(
                "Return URL không hợp lệ. Chỉ chấp nhận HTTPS hoặc localhost cho môi trường development"
            );
        }
        
        return returnUrl;
    }
    
    /**
     * Build payment parameters map
     */
    private Map<String, String> buildPaymentParams(Long orderId, BigDecimal amount, 
                                                    String ipAddress, String returnUrl) {
        Map<String, String> vnpParams = new HashMap<>();
        
        // Basic parameters
        vnpParams.put("vnp_Version", VNP_VERSION);
        vnpParams.put("vnp_Command", VNP_COMMAND);
        vnpParams.put("vnp_TmnCode", vnpayConfig.getVnp_TmnCode());
        
        // Amount (VNPay requires amount * 100)
        BigDecimal amountInCents = amount.multiply(new BigDecimal(AMOUNT_MULTIPLIER));
        vnpParams.put("vnp_Amount", String.valueOf(amountInCents.longValue()));
        vnpParams.put("vnp_CurrCode", CURRENCY_CODE);
        
        // Transaction reference
        vnpParams.put("vnp_TxnRef", String.valueOf(orderId));
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang #" + orderId);
        vnpParams.put("vnp_OrderType", ORDER_TYPE);
        
        // Locale and return URL
        vnpParams.put("vnp_Locale", LOCALE_VN);
        vnpParams.put("vnp_ReturnUrl", returnUrl);
        vnpParams.put("vnp_IpAddr", ipAddress);
        
        // Timestamps
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(TIMEZONE_GMT7));
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        
        String createDate = formatter.format(calendar.getTime());
        vnpParams.put("vnp_CreateDate", createDate);
        
        calendar.add(Calendar.MINUTE, DEFAULT_PAYMENT_TIMEOUT_MINUTES);
        String expireDate = formatter.format(calendar.getTime());
        vnpParams.put("vnp_ExpireDate", expireDate);
        
        log.debug("Built payment params with {} fields", vnpParams.size());
        return vnpParams;
    }
    
    /**
     * Generate payment URL with secure hash
     */
    private String generatePaymentUrl(Map<String, String> vnpParams) {
        // Sort parameters by key
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
        
        // Build hash data and query string
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        
        Iterator<String> iterator = fieldNames.iterator();
        while (iterator.hasNext()) {
            String fieldName = iterator.next();
            String fieldValue = vnpParams.get(fieldName);
            
            if (fieldValue != null && !fieldValue.isEmpty()) {
                // Build hash data
                hashData.append(fieldName)
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                
                // Build query string
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
                     .append('=')
                     .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                
                if (iterator.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        
        // Generate secure hash
        String secureHash = VNPAYConfig.hmacSHA512(vnpayConfig.getVnp_HashSecret(), hashData.toString());
        
        // Build final payment URL
        String queryUrl = query.toString();
        queryUrl += "&vnp_SecureHash=" + secureHash;
        
        String paymentUrl = vnpayConfig.getVnp_PayUrl() + "?" + queryUrl;
        
        log.debug("Generated payment URL with secure hash");
        return paymentUrl;
    }
    
    /**
     * Build hash data from parameters
     */
    private String buildHashData(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        
        StringBuilder hashData = new StringBuilder();
        Iterator<String> iterator = fieldNames.iterator();
        
        while (iterator.hasNext()) {
            String fieldName = iterator.next();
            String fieldValue = params.get(fieldName);
            
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName)
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                
                if (iterator.hasNext()) {
                    hashData.append('&');
                }
            }
        }
        
        return hashData.toString();
    }
    
    /**
     * Parse amount from VNPay format (amount * 100) back to VND
     */
    private BigDecimal parseAmount(String amountStr) {
        try {
            if (amountStr == null || amountStr.trim().isEmpty()) {
                return BigDecimal.ZERO;
            }
            long amountInCents = Long.parseLong(amountStr);
            return new BigDecimal(amountInCents).divide(new BigDecimal(AMOUNT_MULTIPLIER));
        } catch (NumberFormatException e) {
            log.warn("Failed to parse amount: {}", amountStr);
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Get user-friendly message for VNPay response code
     */
    private String getResponseMessage(String responseCode) {
        if (responseCode == null) {
            return "Không xác định được trạng thái giao dịch";
        }
        
        return switch (responseCode) {
            case "00" -> "Giao dịch thành công";
            case "07" -> "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường)";
            case "09" -> "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng";
            case "10" -> "Giao dịch không thành công do: Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần";
            case "11" -> "Giao dịch không thành công do: Đã hết hạn chờ thanh toán. Xin quý khách vui lòng thực hiện lại giao dịch";
            case "12" -> "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng bị khóa";
            case "13" -> "Giao dịch không thành công do Quý khách nhập sai mật khẩu xác thực giao dịch (OTP)";
            case "24" -> "Giao dịch không thành công do: Khách hàng hủy giao dịch";
            case "51" -> "Giao dịch không thành công do: Tài khoản của quý khách không đủ số dư để thực hiện giao dịch";
            case "65" -> "Giao dịch không thành công do: Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày";
            case "75" -> "Ngân hàng thanh toán đang bảo trì";
            case "79" -> "Giao dịch không thành công do: KH nhập sai mật khẩu thanh toán quá số lần quy định";
            default -> "Giao dịch thất bại. Mã lỗi: " + responseCode;
        };
    }
}
