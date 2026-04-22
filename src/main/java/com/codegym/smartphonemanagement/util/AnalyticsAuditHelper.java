package com.codegym.smartphonemanagement.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper utility for analytics audit logging.
 * Provides common methods for extracting user and request information.
 * 
 * Requirements: 14.3, 14.4
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AnalyticsAuditHelper {

    private final SecurityAuditLogger securityAuditLogger;

    /**
     * Log an export operation with user and request details
     * 
     * @param request The HTTP request
     * @param exportType The export type (CSV or Excel)
     * @param dataType The type of data being exported
     * @param filters The filter parameters applied
     */
    public void logExport(HttpServletRequest request, String exportType, 
                         String dataType, String filters) {
        String username = getCurrentUsername();
        String ipAddress = getClientIpAddress(request);
        
        securityAuditLogger.logAnalyticsExport(username, exportType, dataType, filters, ipAddress);
        
        log.debug("Export logged - User: {}, Type: {}, Data: {}, Filters: {}", 
                username, exportType, dataType, filters);
    }

    /**
     * Get current authenticated username
     * @return Username or "anonymous" if not authenticated
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() 
            && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return "anonymous";
    }

    /**
     * Get client IP address from request.
     * Checks X-Forwarded-For header first (for proxied requests),
     * then falls back to remote address.
     * 
     * @param request The HTTP request
     * @return Client IP address
     */
    public String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Build filter string from parameters for audit logging
     * 
     * @param params Variable number of key-value pairs
     * @return Formatted filter string
     */
    public String buildFilterString(Object... params) {
        if (params == null || params.length == 0) {
            return "none";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < params.length; i += 2) {
            if (i > 0) {
                sb.append(", ");
            }
            String key = params[i].toString();
            Object value = (i + 1 < params.length) ? params[i + 1] : "null";
            sb.append(key).append("=").append(value != null ? value : "all");
        }
        return sb.toString();
    }
}
