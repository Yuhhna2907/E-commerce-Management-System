package com.codegym.smartphonemanagement.config;

import com.codegym.smartphonemanagement.util.SecurityAuditLogger;
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

        // Redirect to login page with session expired message
        event.getResponse().sendRedirect("/login?session=expired");
    }
}
