package com.codegym.smartphonemanagement.config;

import com.codegym.smartphonemanagement.util.SecurityAuditLogger;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.ServletException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.session.SessionInformationExpiredEvent;
import org.springframework.security.web.session.SessionInformationExpiredStrategy;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom strategy for handling expired sessions
 * Triggered when a user's session is invalidated due to concurrent session limits
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomSessionInformationExpiredStrategy implements SessionInformationExpiredStrategy {

    private final SecurityAuditLogger securityAuditLogger;

    @Override
    public void onExpiredSessionDetected(SessionInformationExpiredEvent event) 
            throws IOException, ServletException {
        
        String username = event.getSessionInformation().getPrincipal().toString();
        String sessionId = event.getSessionInformation().getSessionId();

        log.info("Session expired for user: {} (Session ID: {})", username, sessionId);

        // Log to security audit
        securityAuditLogger.logSessionExpired(username, sessionId);

        expireCookie(event, "JSESSIONID");
        expireCookie(event, "remember-me");

        // Redirect to login page with session expired message
        event.getResponse().sendRedirect("/login?session=expired");
    }

    private void expireCookie(SessionInformationExpiredEvent event, String cookieName) {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly("JSESSIONID".equals(cookieName));
        event.getResponse().addCookie(cookie);
    }
}
