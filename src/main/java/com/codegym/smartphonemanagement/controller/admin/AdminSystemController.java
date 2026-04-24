package com.codegym.smartphonemanagement.controller.admin;

import com.codegym.smartphonemanagement.configuration.ActiveSessionListener;
import com.codegym.smartphonemanagement.configuration.FeatureFlagsConfig;
import com.codegym.smartphonemanagement.model.OrderStatus;
import com.codegym.smartphonemanagement.repository.user.OrderRepository;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/setup")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminSystemController {

    private final FeatureFlagsConfig featureFlags;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    // ======================== TRANG SETUP ========================

    @GetMapping
    public String setupPage(Model model) {
        model.addAttribute("pageTitle", "setup");
        model.addAttribute("featureFlags", featureFlags);

        // JVM Stats
        Runtime runtime = Runtime.getRuntime();
        long maxMem = runtime.maxMemory() / (1024 * 1024);
        long usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        int cpuCores = runtime.availableProcessors();
        int memPercent = (int) ((usedMem * 100) / maxMem);

        model.addAttribute("maxMemory", maxMem);
        model.addAttribute("usedMemory", usedMem);
        model.addAttribute("memPercent", memPercent);
        model.addAttribute("cpuCores", cpuCores);

        // System info
        model.addAttribute("javaVersion", System.getProperty("java.version"));
        model.addAttribute("osName", System.getProperty("os.name"));
        model.addAttribute("osVersion", System.getProperty("os.version"));
        model.addAttribute("onlineUsers", ActiveSessionListener.getActiveSessionCount());

        return "admin/setup/index";
    }

    // ======================== API: FEATURE FLAGS ========================

    @PostMapping("/flags")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateFlags(@RequestBody Map<String, Boolean> flags) {
        if (flags.containsKey("loyaltySystemEnabled")) featureFlags.setLoyaltySystemEnabled(flags.get("loyaltySystemEnabled"));
        if (flags.containsKey("walletSystemEnabled")) featureFlags.setWalletSystemEnabled(flags.get("walletSystemEnabled"));
        if (flags.containsKey("recommendationEngineEnabled")) featureFlags.setRecommendationEngineEnabled(flags.get("recommendationEngineEnabled"));
        if (flags.containsKey("searchAnalyticsEnabled")) featureFlags.setSearchAnalyticsEnabled(flags.get("searchAnalyticsEnabled"));
        if (flags.containsKey("emailNotificationsEnabled")) featureFlags.setEmailNotificationsEnabled(flags.get("emailNotificationsEnabled"));
        if (flags.containsKey("broadcastEnabled")) featureFlags.setBroadcastEnabled(flags.get("broadcastEnabled"));
        if (flags.containsKey("bruteForceProtectionEnabled")) featureFlags.setBruteForceProtectionEnabled(flags.get("bruteForceProtectionEnabled"));
        if (flags.containsKey("adminNotificationsEnabled")) featureFlags.setAdminNotificationsEnabled(flags.get("adminNotificationsEnabled"));
        if (flags.containsKey("maintenanceMode")) featureFlags.setMaintenanceMode(flags.get("maintenanceMode"));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cấu hình đã được cập nhật!");
        return ResponseEntity.ok(response);
    }

    // ======================== API: HEADER LIVE DATA ========================

    @GetMapping("/api/header-stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getHeaderStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        BigDecimal todayRevenue = orderRepository.sumGMVInPeriod(startOfDay, endOfDay);
        long todayOrders = orderRepository.countOrdersInPeriod(startOfDay, endOfDay);
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        int onlineUsers = ActiveSessionListener.getActiveSessionCount();

        Map<String, Object> stats = new HashMap<>();
        stats.put("todayRevenue", todayRevenue != null ? todayRevenue : BigDecimal.ZERO);
        stats.put("todayOrders", todayOrders);
        stats.put("pendingOrders", pendingOrders);
        stats.put("onlineUsers", onlineUsers);
        stats.put("systemHealthy", true); // Nếu API trả về được → hệ thống đang khỏe

        return ResponseEntity.ok(stats);
    }
}
