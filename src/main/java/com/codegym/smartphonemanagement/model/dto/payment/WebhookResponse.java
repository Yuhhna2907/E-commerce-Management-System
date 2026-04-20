package com.codegym.smartphonemanagement.model.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for webhook response to VNPay
 * Represents the response format expected by VNPay IPN system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookResponse {
    
    /**
     * Response code for VNPay
     * "00" = Success, processing completed
     * "99" = Failure, processing failed
     */
    private String RspCode;
    
    /**
     * Response message for VNPay
     * Human-readable description of the processing result
     */
    private String Message;
    
    /**
     * Create success response
     */
    public static WebhookResponse success() {
        return new WebhookResponse("00", "Confirm Success");
    }
    
    /**
     * Create failure response
     */
    public static WebhookResponse failure(String message) {
        return new WebhookResponse("99", message != null ? message : "Processing Failed");
    }
}