package com.codegym.smartphonemanagement.service.security;

import com.codegym.smartphonemanagement.model.*;
import com.codegym.smartphonemanagement.service.notification.AdminNotificationService;
import com.codegym.smartphonemanagement.repository.LoginAttemptRepository;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import com.codegym.smartphonemanagement.util.SecurityAuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);
    private static final int CLEANUP_DAYS = 90;

    private final LoginAttemptRepository loginAttemptRepository;
    private final UserRepository userRepository;
    private final SecurityAuditLogger securityAuditLogger;
    private final AdminNotificationService adminNotificationService;

    @Override
    @Transactional
    public void recordLoginAttempt(String username, boolean success, String ipAddress, String userAgent) {
        LoginAttempt attempt = LoginAttempt.builder()
                .username(username)
                .success(success)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .attemptTime(LocalDateTime.now())
                .build();

        loginAttemptRepository.save(attempt);

        if (!success) {
            incrementFailedAttempts(username);
        }

        log.debug("Recorded {} login attempt for user: {} from IP: {}",
                success ? "successful" : "failed", username, ipAddress);
    }

    @Override
    public boolean isAccountLocked(String usernameOrEmail) {
        // Try to find user by username first, then by email
        Optional<User> userOpt = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail));
        
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        LocalDateTime lockoutTime = user.getLockoutTime();

        if (lockoutTime == null) {
            return false;
        }

        // Check if lockout has expired
        if (lockoutTime.isBefore(LocalDateTime.now())) {
            // Auto-unlock if lockout period has passed
            unlockAccount(user.getUsername()); // Use actual username for unlock
            return false;
        }

        return true;
    }

    @Override
    @Transactional
    public void resetFailedAttempts(String usernameOrEmail) {
        // Try to find user by username first, then by email
        Optional<User> userOpt = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail));
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setFailedLoginAttempts(0);
            user.setLockoutTime(null);
            userRepository.save(user);
            log.debug("Reset failed attempts for user: {}", user.getUsername());
        }
    }

    @Override
    @Transactional
    public void lockAccount(String usernameOrEmail) {
        // Try to find user by username first, then by email
        Optional<User> userOpt = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail));
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            LocalDateTime lockUntil = LocalDateTime.now().plus(LOCKOUT_DURATION);
            user.setLockoutTime(lockUntil);
            userRepository.save(user);

            securityAuditLogger.logAccountLocked(
                    user.getUsername(),
                    "Exceeded maximum failed login attempts (" + MAX_FAILED_ATTEMPTS + ")",
                    LOCKOUT_DURATION
            );

            // [NEW] Notify Admin
            adminNotificationService.notify(
                    "Cảnh báo bảo mật: Tài khoản bị khóa",
                    "Tài khoản \"" + user.getUsername() + "\" vừa bị khóa 15 phút do nhập sai mật khẩu quá 5 lần.",
                    AdminNotificationType.SECURITY_ALERT,
                    NotificationPriority.CRITICAL,
                    "/admin/users?search=" + user.getUsername()
            );

            log.warn("Account locked for user: {} until {}", user.getUsername(), lockUntil);
        }
    }

    @Override
    @Transactional
    public void unlockAccount(String usernameOrEmail) {
        // Try to find user by username first, then by email
        Optional<User> userOpt = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail));
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLockoutTime(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);

            securityAuditLogger.logAccountUnlocked(user.getUsername(), "Lockout period expired");
            log.info("Account unlocked for user: {}", user.getUsername());
        }
    }

    @Override
    public List<LoginAttempt> getFailedAttemptsForUser(String username, LocalDateTime since) {
        return loginAttemptRepository.findByUsernameAndAttemptTimeAfter(username, since)
                .stream()
                .filter(attempt -> !attempt.isSuccess())
                .toList();
    }

    @Override
    public List<LoginAttempt> getAttemptsFromIp(String ipAddress, LocalDateTime since) {
        return loginAttemptRepository.findByIpAddressAndAttemptTimeAfter(ipAddress, since);
    }

    @Override
    @Transactional
    public void cleanupOldAttempts() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(CLEANUP_DAYS);
        loginAttemptRepository.deleteByAttemptTimeBefore(cutoffDate);
        log.info("Cleaned up login attempts older than {} days", CLEANUP_DAYS);
    }

    /**
     * Increment the failed login attempts counter for a user
     * If the counter reaches MAX_FAILED_ATTEMPTS, lock the account
     */
    private void incrementFailedAttempts(String usernameOrEmail) {
        // Try to find user by username first, then by email
        Optional<User> userOpt = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail));
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            int currentAttempts = user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0;
            user.setFailedLoginAttempts(currentAttempts + 1);
            userRepository.save(user);

            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                lockAccount(user.getUsername()); // Use actual username for lock
            }
        }
    }
}
