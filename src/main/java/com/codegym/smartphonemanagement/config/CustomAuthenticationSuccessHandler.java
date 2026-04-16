package com.codegym.smartphonemanagement.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        System.out.println("=== CustomAuthenticationSuccessHandler: Đăng nhập thành công ===");
        System.out.println("=== Username: " + authentication.getName());
        
        // Lấy danh sách các role của user
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_ADMIN"));

        System.out.println("=== isAdmin: " + isAdmin);

        // Điều hướng dựa trên role
        if (isAdmin) {
            System.out.println("=== Điều hướng đến /admin/dashboard ===");
            response.sendRedirect("/admin/dashboard");
        } else {
            System.out.println("=== Điều hướng đến /user/products ===");
            response.sendRedirect("/user/products");
        }
    }
}

