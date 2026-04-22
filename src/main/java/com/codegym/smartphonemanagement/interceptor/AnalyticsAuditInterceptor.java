package com.codegym.smartphonemanagement.interceptor;

import com.codegym.smartphonemanagement.util.SecurityAuditLogger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor for auditing analytics page access.
 * Logs all access attempts to analytics endpoints with user information.
 * 
 * Requirements: 14.3, 14.4
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AnalyticsAuditInterceptor implements HandlerInterceptor {

    private final SecurityAuditLogger securityAuditLogger;

    /**
     * Pre-handle method to log analytics page access before request processing.
     * Captures username, page path, and IP address for audit trail.
     * 
     * @param request The HTTP request
     * @param response The HTTP response
     * @param handler The handler
     * @return true to continue processing
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Get authenticated username
        String username = getCurrentUsername();
        
        // Get request path
        String pagePath = request.getRequestURI();
        
        // Get client IP address
        String ipAddress = getClientIpAddress(request);
        
        // Log analytics page access
        securityAuditLogger.logAnalyticsPageAccess(username, pagePath, ipAddress);
        
        // Log filter parameters if present
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            securityAuditLogger.logAnalyticsFilter(username, pagePath, queryString);
        }
        
        log.debug("Analytics page access logged - User: {}, Path: {}, IP: {}", 
                username, pagePath, ipAddress);
        
        return true;
    }

    /**
     * Get current authenticated username
     * @return Username or "anonymous" if not authenticated
     */
    private String getCurrentUsername() {
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
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
