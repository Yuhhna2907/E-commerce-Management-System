package com.codegym.smartphonemanagement.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Component for validating payment configuration on application startup
 * Validates retry, webhook, and refund configuration properties
 */
@Component
@Slf4j
public class PaymentConfigurationValidator {

    // Retry Configuration
    @Value("${app.payment.retry.max-attempts:3}")
    private int retryMaxAttempts;

    @Value("${app.payment.retry.initial-delay:1000}")
    private long retryInitialDelay;

    @Value("${app.payment.retry.multiplier:2.0}")
    private double retryMultiplier;

    @Value("${app.payment.retry.max-delay:10000}")
    private long retryMaxDelay;

    // Webhook Configuration
    @Value("${app.payment.webhook.async-pool-size:10}")
    private int webhookPoolSize;

    @Value("${app.payment.webhook.timeout:30000}")
    private long webhookTimeout;

    @Value("${app.payment.webhook.ip-whitelist:}")
    private String webhookIpWhitelist;

    // Refund Configuration
    @Value("${app.payment.refund.timeout:30000}")
    private long refundTimeout;

    @Value("${app.payment.refund.max-amount-check:true}")
    private boolean refundMaxAmountCheck;

    // VNPay Configuration
    @Value("${vnpay.apiUrl:}")
    private String vnpayApiUrl;

    @Value("${vnpay.refundUrl:}")
    private String vnpayRefundUrl;

    @Value("${vnpay.tmnCode:}")
    private String vnpayTmnCode;

    @Value("${vnpay.hashSecret:}")
    private String vnpayHashSecret;

    @Value("${vnpay.url:}")
    private String vnpayUrl;

    @Value("${vnpay.returnUrl:}")
    private String vnpayReturnUrl;

    /**
     * Validate configuration on application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration() {
        log.info("Validating payment configuration...");

        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // Validate retry configuration
        validateRetryConfiguration(warnings, errors);

        // Validate webhook configuration
        validateWebhookConfiguration(warnings, errors);

        // Validate refund configuration
        validateRefundConfiguration(warnings, errors);

        // Validate VNPay configuration
        validateVNPayConfiguration(warnings, errors);

        // Log results
        logValidationResults(warnings, errors);

        // Throw exception if critical errors found
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Payment configuration validation failed: " + String.join(", ", errors));
        }
    }

    /**
     * Validate retry configuration parameters
     */
    private void validateRetryConfiguration(List<String> warnings, List<String> errors) {
        if (retryMaxAttempts < 1 || retryMaxAttempts > 10) {
            warnings.add("Retry max attempts should be between 1 and 10, current: " + retryMaxAttempts);
        }

        if (retryInitialDelay < 100 || retryInitialDelay > 10000) {
            warnings.add("Retry initial delay should be between 100ms and 10s, current: " + retryInitialDelay + "ms");
        }

        if (retryMultiplier < 1.0 || retryMultiplier > 5.0) {
            warnings.add("Retry multiplier should be between 1.0 and 5.0, current: " + retryMultiplier);
        }

        if (retryMaxDelay < retryInitialDelay) {
            errors.add("Retry max delay (" + retryMaxDelay + "ms) must be >= initial delay (" + retryInitialDelay + "ms)");
        }

        if (retryMaxDelay > 60000) {
            warnings.add("Retry max delay is very high: " + retryMaxDelay + "ms, consider reducing for better user experience");
        }
    }

    /**
     * Validate webhook configuration parameters
     */
    private void validateWebhookConfiguration(List<String> warnings, List<String> errors) {
        if (webhookPoolSize < 1 || webhookPoolSize > 100) {
            warnings.add("Webhook pool size should be between 1 and 100, current: " + webhookPoolSize);
        }

        if (webhookTimeout < 5000 || webhookTimeout > 120000) {
            warnings.add("Webhook timeout should be between 5s and 120s, current: " + webhookTimeout + "ms");
        }

        if (webhookIpWhitelist != null && !webhookIpWhitelist.trim().isEmpty()) {
            // Validate IP whitelist format
            String[] ips = webhookIpWhitelist.split(",");
            for (String ip : ips) {
                if (!isValidIpAddress(ip.trim())) {
                    warnings.add("Invalid IP address in whitelist: " + ip.trim());
                }
            }
        }
    }

    /**
     * Validate refund configuration parameters
     */
    private void validateRefundConfiguration(List<String> warnings, List<String> errors) {
        if (refundTimeout < 5000 || refundTimeout > 300000) {
            warnings.add("Refund timeout should be between 5s and 300s, current: " + refundTimeout + "ms");
        }

        // Log refund configuration
        log.info("Refund configuration: timeout={}ms, maxAmountCheck={}", refundTimeout, refundMaxAmountCheck);
    }

    /**
     * Validate VNPay configuration parameters
     */
    private void validateVNPayConfiguration(List<String> warnings, List<String> errors) {
        if (vnpayTmnCode == null || vnpayTmnCode.trim().isEmpty()) {
            errors.add("VNPay terminal code (vnpay.tmnCode) is required");
        }

        if (vnpayHashSecret == null || vnpayHashSecret.trim().isEmpty()) {
            errors.add("VNPay hash secret (vnpay.hashSecret) is required");
        } else if (vnpayHashSecret.length() < 32) {
            warnings.add("VNPay hash secret should be at least 32 characters for security");
        }

        if (vnpayUrl == null || vnpayUrl.trim().isEmpty()) {
            errors.add("VNPay payment URL (vnpay.url) is required");
        } else if (!vnpayUrl.startsWith("https://")) {
            warnings.add("VNPay payment URL should use HTTPS for security");
        }

        if (vnpayReturnUrl == null || vnpayReturnUrl.trim().isEmpty()) {
            errors.add("VNPay return URL (vnpay.returnUrl) is required");
        }

        if (vnpayApiUrl == null || vnpayApiUrl.trim().isEmpty()) {
            warnings.add("VNPay API URL not configured, using default sandbox URL");
        }

        if (vnpayRefundUrl == null || vnpayRefundUrl.trim().isEmpty()) {
            warnings.add("VNPay refund URL not configured, using API URL as default");
        }
    }

    /**
     * Log validation results
     */
    private void logValidationResults(List<String> warnings, List<String> errors) {
        if (errors.isEmpty() && warnings.isEmpty()) {
            log.info("Payment configuration validation completed successfully");
            return;
        }

        if (!warnings.isEmpty()) {
            log.warn("Payment configuration warnings:");
            warnings.forEach(warning -> log.warn("  - {}", warning));
        }

        if (!errors.isEmpty()) {
            log.error("Payment configuration errors:");
            errors.forEach(error -> log.error("  - {}", error));
        }
    }

    /**
     * Validate IP address format (basic validation)
     */
    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        // Basic IPv4 validation
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Get current configuration summary for logging
     */
    public String getConfigurationSummary() {
        return String.format(
            "Payment Configuration Summary:\n" +
            "  Retry: maxAttempts=%d, initialDelay=%dms, multiplier=%.1f, maxDelay=%dms\n" +
            "  Webhook: poolSize=%d, timeout=%dms, ipWhitelist='%s'\n" +
            "  Refund: timeout=%dms, maxAmountCheck=%s\n" +
            "  VNPay: apiUrl='%s', tmnCode='%s'",
            retryMaxAttempts, retryInitialDelay, retryMultiplier, retryMaxDelay,
            webhookPoolSize, webhookTimeout, webhookIpWhitelist,
            refundTimeout, refundMaxAmountCheck,
            vnpayApiUrl, vnpayTmnCode != null ? vnpayTmnCode.substring(0, Math.min(4, vnpayTmnCode.length())) + "***" : "null"
        );
    }
}