package com.codegym.smartphonemanagement.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Centralized security audit logging utility
 * All security events are logged to a dedicated logger named "SECURITY_AUDIT"
 */
@Component
public class SecurityAuditLogger {

    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");

    /**
     * Log successful login event
     */
    public void logLoginSuccess(String username, String ipAddress, String userAgent) {
        auditLogger.info("[LOGIN_SUCCESS] username={} ip={} userAgent={}",
                username, ipAddress, userAgent);
    }

    /**
     * Log failed login event
     */
    public void logLoginFailure(String username, String ipAddress, String reason) {
        auditLogger.warn("[LOGIN_FAILURE] username={} ip={} reason={}",
                username, ipAddress, reason);
    }

    /**
     * Log logout event
     */
    public void logLogout(String username, Duration sessionDuration) {
        auditLogger.info("[LOGOUT] username={} sessionDuration={}",
                username, formatDuration(sessionDuration));
    }

    /**
     * Log account locked event
     */
    public void logAccountLocked(String username, String reason, Duration lockDuration) {
        auditLogger.warn("[ACCOUNT_LOCKED] username={} reason={} lockDuration={}",
                username, reason, formatDuration(lockDuration));
    }

    /**
     * Log account unlocked event
     */
    public void logAccountUnlocked(String username, String reason) {
        auditLogger.info("[ACCOUNT_UNLOCKED] username={} reason={}",
                username, reason);
    }

    /**
     * Log authorization denied event
     */
    public void logAuthorizationDenied(String username, String resource, String requiredRole) {
        auditLogger.warn("[AUTHORIZATION_DENIED] username={} resource={} requiredRole={}",
                username, resource, requiredRole);
    }

    /**
     * Log password changed event
     */
    public void logPasswordChanged(String username) {
        auditLogger.info("[PASSWORD_CHANGED] username={}", username);
    }

    /**
     * Log CSRF token error
     */
    public void logCsrfError(String username, String requestedUrl) {
        auditLogger.warn("[CSRF_ERROR] username={} requestedUrl={}",
                username != null ? username : "anonymous", requestedUrl);
    }

    /**
     * Log session expired event
     */
    public void logSessionExpired(String username) {
        auditLogger.info("[SESSION_EXPIRED] username={}", username);
    }

    /**
     * Log session expired event with session ID
     */
    public void logSessionExpired(String username, String sessionId) {
        auditLogger.info("[SESSION_EXPIRED] username={} sessionId={}", username, sessionId);
    }

    /**
     * Log access denied event
     */
    public void logAccessDenied(String username, String requestedUrl, String ipAddress) {
        auditLogger.warn("[ACCESS_DENIED] username={} requestedUrl={} ip={}",
                username, requestedUrl, ipAddress);
    }

    /**
     * Format duration for logging
     */
    private String formatDuration(Duration duration) {
        if (duration == null) {
            return "unknown";
        }
        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes + " minutes";
        }
        long hours = duration.toHours();
        return hours + " hours";
    }
}
