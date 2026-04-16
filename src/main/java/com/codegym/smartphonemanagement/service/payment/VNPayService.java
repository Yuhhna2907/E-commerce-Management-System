package com.codegym.smartphonemanagement.service.payment;

import com.codegym.smartphonemanagement.config.payment.VNPAYConfig;
import com.codegym.smartphonemanagement.model.Order;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VNPayService {

    private final VNPAYConfig vnpayConfig;

    public VNPayService(VNPAYConfig vnpayConfig) {
        this.vnpayConfig = vnpayConfig;
    }

    public String createPaymentUrl(Long orderId, BigDecimal amount, String ipAddress) {
        // FIX #15: Validate return URL trước khi tạo payment URL
        String returnUrl = vnpayConfig.getVnp_ReturnUrl();
        if (returnUrl == null || returnUrl.trim().isEmpty()) {
            throw new RuntimeException("Return URL không được cấu hình");
        }
        
        // Validate return URL format để đảm bảo security
        if (!returnUrl.startsWith("http://localhost") && 
            !returnUrl.startsWith("https://localhost") &&
            !returnUrl.startsWith("https://")) {
            throw new RuntimeException("Return URL không hợp lệ. Chỉ chấp nhận HTTPS hoặc localhost");
        }
        
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String orderType = "other";
        
        // Amount = Total Price * 100
        BigDecimal amountBig = amount.multiply(new BigDecimal(100));
        long amountLong = amountBig.longValue();
        
        String vnp_TxnRef = String.valueOf(orderId);
        String vnp_IpAddr = ipAddress;
        String vnp_TmnCode = vnpayConfig.getVnp_TmnCode();

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amountLong));
        vnp_Params.put("vnp_CurrCode", "VND");
        
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", orderType);

        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnpayConfig.getVnp_ReturnUrl());
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        
        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                //Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                //Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = VNPAYConfig.hmacSHA512(vnpayConfig.getVnp_HashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = vnpayConfig.getVnp_PayUrl() + "?" + queryUrl;
        
        return paymentUrl;
    }

    public boolean verifySignature(Map<String, String> requestParams) {
        String vnp_SecureHash = requestParams.get("vnp_SecureHash");
        if (vnp_SecureHash == null) {
            return false;
        }
        
        Map<String, String> vnp_Params = new HashMap<>(requestParams);
        vnp_Params.remove("vnp_SecureHashType");
        vnp_Params.remove("vnp_SecureHash");
        
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        try {
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = (String) itr.next();
                String fieldValue = (String) vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
        String signValue = VNPAYConfig.hmacSHA512(vnpayConfig.getVnp_HashSecret(), hashData.toString());
        return signValue.equals(vnp_SecureHash);
    }
}
