package com.codegym.smartphonemanagement.service.payment;

import com.codegym.smartphonemanagement.config.payment.VNPAYConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.TreeMap;

/**
 * Component for verifying HMAC-SHA512 signatures from VNPay
 * Provides secure signature validation with constant-time comparison to prevent timing attacks
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SignatureVerifier {

    private final VNPAYConfig vnpayConfig;

    /**
     * Verify HMAC-SHA512 signature for VNPay webhook or API response
     * Uses constant-time comparison to prevent timing attacks
     * 
     * @param params Map of parameters from VNPay (excluding vnp_SecureHash)
     * @param providedSignature The signature provided by VNPay
     * @return true if signature is valid, false otherwise
     */
    public boolean verifyHmacSHA512(Map<String, String> params, String providedSignature) {
        if (params == null || providedSignature == null) {
            log.warn("Signature verification failed: null parameters or signature");
            return false;
        }

        try {
            // Build hash data from parameters (excluding vnp_SecureHash)
            String hashData = buildHashData(params);
            
            // Calculate expected signature
            String expectedSignature = VNPAYConfig.hmacSHA512(vnpayConfig.getVnp_HashSecret(), hashData);
            
            // Use constant-time comparison to prevent timing attacks
            boolean isValid = constantTimeEquals(expectedSignature, providedSignature);
            
            if (isValid) {
                log.debug("Signature verification successful");
            } else {
                log.warn("Signature verification failed: signature mismatch");
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Signature verification failed due to exception: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Build hash data string from parameters
     * Sorts parameters alphabetically and excludes vnp_SecureHash
     * 
     * @param params Map of parameters from VNPay
     * @return Hash data string for signature calculation
     */
    private String buildHashData(Map<String, String> params) {
        // Use TreeMap to sort parameters alphabetically
        TreeMap<String, String> sortedParams = new TreeMap<>();
        
        // Add all parameters except vnp_SecureHash
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            // Skip vnp_SecureHash and empty values
            if (!"vnp_SecureHash".equals(key) && value != null && !value.isEmpty()) {
                sortedParams.put(key, value);
            }
        }
        
        // Build hash data string
        StringBuilder hashData = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (hashData.length() > 0) {
                hashData.append("&");
            }
            hashData.append(entry.getKey()).append("=").append(entry.getValue());
        }
        
        String result = hashData.toString();
        log.debug("Built hash data: {}", result);
        
        return result;
    }

    /**
     * Constant-time string comparison to prevent timing attacks
     * Compares two strings in constant time regardless of where they differ
     * 
     * @param a First string
     * @param b Second string
     * @return true if strings are equal, false otherwise
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        
        // If lengths are different, still compare to prevent timing attacks
        int length = Math.max(a.length(), b.length());
        int result = a.length() ^ b.length(); // XOR lengths
        
        // Compare each character position
        for (int i = 0; i < length; i++) {
            char charA = i < a.length() ? a.charAt(i) : 0;
            char charB = i < b.length() ? b.charAt(i) : 0;
            result |= charA ^ charB; // XOR characters
        }
        
        // Return true only if result is 0 (all XORs were 0)
        return result == 0;
    }
}