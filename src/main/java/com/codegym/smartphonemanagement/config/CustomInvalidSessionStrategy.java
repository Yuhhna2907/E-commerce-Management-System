package com.codegym.smartphonemanagement.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.session.InvalidSessionStrategy;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Clears stale authentication cookies before redirecting the user
 * to the login page after a session timeout/invalid session.
 */
@Component
@Slf4j
public class CustomInvalidSessionStrategy implements InvalidSessionStrategy {

    @Override
    public void onInvalidSessionDetected(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        log.info("Invalid session detected for request: {}", request.getRequestURI());

        expireCookie(response, "JSESSIONID");
        expireCookie(response, "remember-me");

        response.sendRedirect("/login?session=expired");
    }

    private void expireCookie(HttpServletResponse response, String cookieName) {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly("JSESSIONID".equals(cookieName));
        response.addCookie(cookie);
    }
}
