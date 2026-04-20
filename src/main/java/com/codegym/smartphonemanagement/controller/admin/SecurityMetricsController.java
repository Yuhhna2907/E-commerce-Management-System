package com.codegym.smartphonemanagement.controller.admin;

import com.codegym.smartphonemanagement.model.LoginAttempt;
import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.repository.LoginAttemptRepository;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import com.codegym.smartphonemanagement.service.security.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for security metrics
 * Provides login attempt statistics and account lockout information
 * Secured with ROLE_ADMIN - only administrators can access these metrics
 */
@RestController
@RequestMapping("/api/admin/security/metrics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SecurityMetricsController {

    private final LoginAttemptRepository loginAttemptRepository;
    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;

    /**
     * Get overall login attempt metrics
     * @param hours number of hours to look back (default: 24)
     * @return metrics including total attempts, failed attempts, and locked accounts
     */
    @GetMapping("/login-attempts")
    public ResponseEntity<Map<String, Object>> getLoginAttemptMetrics(
            @RequestParam(defaultValue = "24") int hours) {
        
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<LoginAttempt> allAttempts = loginAttemptRepository.findAll()
                .stream()
                .filter(attempt -> attempt.getAttemptTime().isAfter(since))
                .toList();

        long totalAttempts = allAttempts.size();
        long failedAttempts = allAttempts.stream()
                .filter(attempt -> !attempt.isSuccess())
                .count();
        long successfulAttempts = allAttempts.stream()
                .filter(LoginAttempt::isSuccess)
                .count();

        // Count locked accounts
        long lockedAccounts = userRepository.findAll().stream()
                .filter(User::isAccountLocked)
                .count();

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("timeRange", hours + " hours");
        metrics.put("totalAttempts", totalAttempts);
        metrics.put("successfulAttempts", successfulAttempts);
        metrics.put("failedAttempts", failedAttempts);
        metrics.put("lockedAccounts", lockedAccounts);
        metrics.put("successRate", totalAttempts > 0 ? 
                String.format("%.2f%%", (successfulAttempts * 100.0 / totalAttempts)) : "N/A");

        return ResponseEntity.ok(metrics);
    }

    /**
     * Get list of currently locked accounts
     * @return list of locked user information
     */
    @GetMapping("/locked-accounts")
    public ResponseEntity<List<Map<String, Object>>> getLockedAccounts() {
        List<Map<String, Object>> lockedAccounts = userRepository.findAll().stream()
                .filter(User::isAccountLocked)
                .map(user -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("username", user.getUsername());
                    info.put("lockoutTime", user.getLockoutTime());
                    info.put("failedAttempts", user.getFailedLoginAttempts());
                    return info;
                })
                .toList();

        return ResponseEntity.ok(lockedAccounts);
    }

    /**
     * Get failed login attempts by IP address
     * @param hours number of hours to look back (default: 24)
     * @return map of IP addresses to failed attempt counts
     */
    @GetMapping("/failed-by-ip")
    public ResponseEntity<Map<String, Long>> getFailedAttemptsByIp(
            @RequestParam(defaultValue = "24") int hours) {
        
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<LoginAttempt> failedAttempts = loginAttemptRepository.findAll()
                .stream()
                .filter(attempt -> !attempt.isSuccess() && attempt.getAttemptTime().isAfter(since))
                .toList();

        Map<String, Long> ipCounts = new HashMap<>();
        for (LoginAttempt attempt : failedAttempts) {
            String ip = attempt.getIpAddress();
            if (ip != null) {
                ipCounts.put(ip, ipCounts.getOrDefault(ip, 0L) + 1);
            }
        }

        return ResponseEntity.ok(ipCounts);
    }

    /**
     * Get recent failed login attempts for a specific user
     * @param username the username to query
     * @param hours number of hours to look back (default: 24)
     * @return list of failed attempts
     */
    @GetMapping("/user-attempts")
    public ResponseEntity<List<LoginAttempt>> getUserAttempts(
            @RequestParam String username,
            @RequestParam(defaultValue = "24") int hours) {
        
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<LoginAttempt> attempts = loginAttemptService.getFailedAttemptsForUser(username, since);
        
        return ResponseEntity.ok(attempts);
    }
}
