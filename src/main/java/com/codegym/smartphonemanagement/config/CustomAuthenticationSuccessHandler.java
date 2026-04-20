package com.codegym.smartphonemanagement.config;

import com.codegym.smartphonemanagement.service.security.LoginAttemptService;
import com.codegym.smartphonemanagement.util.SecurityAuditLogger;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final LoginAttemptService loginAttemptService;
    private final SecurityAuditLogger securityAuditLogger;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        String ipAddress = getClientIP(request);
        String userAgent = request.getHeader("User-Agent");

        log.info("Authentication successful for user: {}", username);

        // Record successful login attempt
        loginAttemptService.recordLoginAttempt(username, true, ipAddress, userAgent);
        
        // Reset failed attempts counter
        loginAttemptService.resetFailedAttempts(username);
        
        // Log to security audit
        securityAuditLogger.logLoginSuccess(username, ipAddress, userAgent);

        // Determine redirect URL based on role
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_ADMIN"));

        String redirectUrl = isAdmin ? "/admin/dashboard" : "/user/products";
        log.debug("Redirecting user {} to {}", username, redirectUrl);
        
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

