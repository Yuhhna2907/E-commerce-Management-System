package com.codegym.smartphonemanagement.config;

import com.codegym.smartphonemanagement.util.SecurityAuditLogger;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom access denied handler for handling authorization failures
 * Logs security events and redirects to appropriate error pages
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityAuditLogger securityAuditLogger;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "anonymous";
        String ipAddress = getClientIP(request);
        String requestedUrl = request.getRequestURI();

        log.warn("Access denied for user: {} attempting to access: {} from IP: {}", 
                username, requestedUrl, ipAddress);

        // Log to security audit
        securityAuditLogger.logAccessDenied(username, requestedUrl, ipAddress);

        // Determine if user is authenticated
        boolean isAuthenticated = auth != null && auth.isAuthenticated() 
                && !auth.getPrincipal().equals("anonymousUser");

        // Check if it's an AJAX request
        String requestedWith = request.getHeader("X-Requested-With");
        String acceptHeader = request.getHeader("Accept");
        boolean isAjax = "XMLHttpRequest".equals(requestedWith) 
                || (acceptHeader != null && acceptHeader.contains("application/json"))
                || requestedUrl.startsWith("/api/");

        if (isAjax) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"Bạn không có quyền thực hiện hành động này!\",\"status\":403}");
            return;
        }

        if (isAuthenticated) {
            // Authenticated user tried to access forbidden resource
            response.sendRedirect("/error/403?url=" + requestedUrl);
        } else {
            // Unauthenticated user - redirect to login
            response.sendRedirect("/login?error=unauthorized&returnUrl=" + requestedUrl);
        }
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
