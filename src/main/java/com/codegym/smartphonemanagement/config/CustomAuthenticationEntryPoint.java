package com.codegym.smartphonemanagement.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Redirects unauthenticated users to login and preserves a clear
 * "session expired" signal when stale session/auth cookies are present.
 */
@Component
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        boolean hasStaleSessionState = hasCookie(request, "JSESSIONID")
                || hasCookie(request, "remember-me")
                || request.getRequestedSessionId() != null;

        if (hasStaleSessionState) {
            log.info("Redirecting to login with session expired message for request: {}", request.getRequestURI());
            expireCookie(response, "JSESSIONID");
            expireCookie(response, "remember-me");
            response.sendRedirect("/login?session=expired");
            return;
        }

        // Check if it's an AJAX request
        String requestedWith = request.getHeader("X-Requested-With");
        String acceptHeader = request.getHeader("Accept");
        boolean isAjax = "XMLHttpRequest".equals(requestedWith) 
                || (acceptHeader != null && acceptHeader.contains("application/json"))
                || request.getRequestURI().startsWith("/api/");

        if (isAjax) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"Vui lòng đăng nhập để tiếp tục!\",\"status\":401}");
            return;
        }

        response.sendRedirect("/login");
    }

    private boolean hasCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return true;
            }
        }
        return false;
    }

    private void expireCookie(HttpServletResponse response, String cookieName) {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly("JSESSIONID".equals(cookieName));
        response.addCookie(cookie);
    }
}
