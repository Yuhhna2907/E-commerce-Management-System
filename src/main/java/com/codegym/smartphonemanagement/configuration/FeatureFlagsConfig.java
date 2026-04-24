package com.codegym.smartphonemanagement.configuration;

import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * Feature Flags lưu trữ in-memory.
 * Khi restart server, tất cả sẽ reset về giá trị mặc định.
 * Admin có thể bật/tắt các tính năng từ trang System Setup.
 */
@Component
@Data
public class FeatureFlagsConfig {

    // --- CORE FEATURES ---
    private boolean loyaltySystemEnabled = true;
    private boolean walletSystemEnabled = true;
    private boolean recommendationEngineEnabled = true;
    private boolean searchAnalyticsEnabled = true;

    // --- MARKETING ---
    private boolean emailNotificationsEnabled = true;
    private boolean broadcastEnabled = true;

    // --- SECURITY ---
    private boolean bruteForceProtectionEnabled = true;
    private boolean adminNotificationsEnabled = true;

    // --- SYSTEM ---
    private boolean maintenanceMode = false;
    private String maintenanceMessage = "Hệ thống đang bảo trì, vui lòng quay lại sau.";
}
