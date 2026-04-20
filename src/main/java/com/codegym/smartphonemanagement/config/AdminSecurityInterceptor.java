package com.codegym.smartphonemanagement.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminSecurityInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();

        // Chỉ áp dụng cho admin pages
        if (requestURI.startsWith("/admin/")) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // Kiểm tra authentication
            boolean isAuthenticated = authentication != null && authentication.isAuthenticated()
                    && !authentication.getPrincipal().equals("anonymousUser");

            // Kiểm tra quyền admin
            boolean isAdmin = isAuthenticated && authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

            // Nếu không phải admin, chuyển hướng về login
            if (!isAdmin) {
                response.sendRedirect("/login?access_denied=true");
                return false;
            }
        }

        return true;
    }
}
