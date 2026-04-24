package com.codegym.smartphonemanagement.interceptor;

import com.codegym.smartphonemanagement.model.SearchLog;
import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.repository.user.SearchLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;

/**
 * Interceptor for tracking user search behavior
 * Logs search queries to search_log table for analytics
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SearchLogInterceptor implements HandlerInterceptor {
    
    private final SearchLogRepository searchLogRepository;
    
    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) throws Exception {
        try {
            // Extract search keyword from request parameters
            String keyword = extractSearchKeyword(request);
            
            // Skip if no valid keyword
            if (keyword == null) {
                return;
            }
            
            // Validate keyword length >= 2 characters
            if (keyword.length() < 2) {
                log.debug("Search keyword too short, skipping log: {}", keyword);
                return;
            }
            
            // Convert keyword to lowercase for consistent aggregation
            keyword = keyword.toLowerCase();
            
            // Sanitize keyword using HtmlUtils.htmlEscape() to prevent XSS
            keyword = HtmlUtils.htmlEscape(keyword);
            
            // Get user from SecurityContextHolder (nullable for anonymous users)
            User user = getCurrentUser();
            
            // Skip logging if user has ROLE_ADMIN or ROLE_SUPER_ADMIN
            if (user != null && isAdminUser()) {
                log.debug("Skipping search log for admin user: {}", user.getUsername());
                return;
            }
            
            // Get session ID from HttpSession
            HttpSession session = request.getSession(true);
            String sessionId = session.getId();
            
            // Create SearchLog entry with searchTimestamp = LocalDateTime.now()
            SearchLog searchLog = SearchLog.builder()
                    .user(user)
                    .searchKeyword(keyword)
                    .searchTimestamp(LocalDateTime.now())
                    .sessionId(sessionId)
                    .converted(false)
                    .build();
            
            searchLogRepository.save(searchLog);
            
            log.debug("Search logged: keyword='{}', user={}, session={}", 
                    keyword, user != null ? user.getUsername() : "anonymous", sessionId);
            
        } catch (Exception e) {
            // Log error but don't fail the request
            log.error("Error logging search query", e);
        }
    }
    
    /**
     * Extract search keyword from request parameter "q" or "search"
     */
    private String extractSearchKeyword(HttpServletRequest request) {
        String keyword = request.getParameter("q");
        if (keyword == null || keyword.trim().isEmpty()) {
            keyword = request.getParameter("search");
        }
        
        if (keyword != null) {
            keyword = keyword.trim();
            if (keyword.isEmpty()) {
                return null;
            }
        }
        
        return keyword;
    }
    
    /**
     * Get current authenticated user from SecurityContextHolder
     * Returns null for anonymous users
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        
        return null;
    }
    
    /**
     * Check if current user has ROLE_ADMIN or ROLE_SUPER_ADMIN
     */
    private boolean isAdminUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_SUPER_ADMIN"));
    }
}
