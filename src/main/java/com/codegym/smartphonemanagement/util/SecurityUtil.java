package com.codegym.smartphonemanagement.util;

import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class for security-related operations
 */
public class SecurityUtil {

    /**
     * Lấy userId từ SecurityContext hiện tại
     */
    public static Long getCurrentUserId(UserRepository userRepository) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() 
                && !authentication.getPrincipal().equals("anonymousUser")) {
            
            Object principal = authentication.getPrincipal();
            
            if (principal instanceof org.springframework.security.core.userdetails.User) {
                String username = ((org.springframework.security.core.userdetails.User) principal).getUsername();
                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User không tồn tại"));
                return user.getId();
            }
        }
        
        throw new RuntimeException("User không được xác thực!");
    }

    /**
     * Lấy User object từ SecurityContext hiện tại
     */
    public static User getCurrentUser(UserRepository userRepository) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() 
                && !authentication.getPrincipal().equals("anonymousUser")) {
            
            Object principal = authentication.getPrincipal();
            
            if (principal instanceof org.springframework.security.core.userdetails.User) {
                String username = ((org.springframework.security.core.userdetails.User) principal).getUsername();
                return userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User không tồn tại"));
            }
        }
        
        throw new RuntimeException("User không được xác thực!");
    }

    /**
     * Kiểm tra xem user có được xác thực không
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() 
                && !authentication.getPrincipal().equals("anonymousUser");
    }

    /**
     * Lấy username từ SecurityContext
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.User) {
                return ((org.springframework.security.core.userdetails.User) principal).getUsername();
            }
        }
        
        return null;
    }
}

