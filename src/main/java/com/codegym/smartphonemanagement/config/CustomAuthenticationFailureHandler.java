package com.codegym.smartphonemanagement.config;

import com.codegym.smartphonemanagement.service.security.LoginAttemptService;
import com.codegym.smartphonemanagement.util.SecurityAuditLogger;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Custom authentication failure handler
 * Records failed login attempts and triggers account lockout after 5 failures
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;
    private final SecurityAuditLogger securityAuditLogger;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        
        String username = request.getParameter("username");
        String ipAddress = getClientIP(request);
        String userAgent = request.getHeader("User-Agent");

        log.warn("Authentication failed for user: {} from IP: {}. Exception: {} - {}",
                username,
                ipAddress,
                exception.getClass().getSimpleName(),
                exception.getMessage());

        // CRITICAL FIX: Only record failed attempt if account is NOT already locked
        // This prevents infinite lock when user tries wrong password while locked
        boolean isAlreadyLocked = loginAttemptService.isAccountLocked(username);
        if (!isAlreadyLocked) {
            loginAttemptService.recordLoginAttempt(username, false, ipAddress, userAgent);
        } else {
            log.debug("Skipping failed attempt recording for already locked account: {}", username);
        }

        // Determine failure reason
        String errorMessage;
        String errorParam;

        if (exception instanceof LockedException) {
            errorMessage = "Account is temporarily locked due to multiple failed login attempts. Please try again later.";
            errorParam = "locked";
            securityAuditLogger.logLoginFailure(username, ipAddress, "Account locked");
        } else if (exception instanceof BadCredentialsException) {
            // Check if this failure triggered lockout (after recording attempt above)
            if (isAlreadyLocked || loginAttemptService.isAccountLocked(username)) {
                errorMessage = "Account has been locked due to multiple failed login attempts. Please try again in 15 minutes.";
                errorParam = "locked";
                securityAuditLogger.logLoginFailure(username, ipAddress, "Account locked after failed attempts");
            } else {
                errorMessage = "Invalid username or password.";
                errorParam = "invalid";
                securityAuditLogger.logLoginFailure(username, ipAddress, "Invalid credentials");
            }
        } else {
            errorMessage = "Authentication failed: " + exception.getMessage();
            errorParam = "error";
            securityAuditLogger.logLoginFailure(username, ipAddress, exception.getMessage());
        }

        // Redirect to login page with error parameter
        String redirectUrl = "/login?error=" + errorParam;
        if (errorMessage != null) {
            redirectUrl += "&message=" + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        }

        log.debug("Redirecting to: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    /**
     * Extract client IP address from request, handling proxy headers
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
