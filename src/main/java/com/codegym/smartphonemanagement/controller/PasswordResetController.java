package com.codegym.smartphonemanagement.controller;

import com.codegym.smartphonemanagement.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller xử lý các request liên quan đến forgot password và reset password
 * Requirements: 3.1, 4.1
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class PasswordResetController {
    
    private final PasswordResetService passwordResetService;
    
    /**
     * Hiển thị form forgot password
     * Requirements: 3.1
     * 
     * @return View forgot-password
     */
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        log.debug("Displaying forgot password form");
        return "register/forgot-password";
    }
    
    /**
     * Xử lý request forgot password
     * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.13, 3.14, 3.15
     * 
     * @param email Email của user yêu cầu reset password
     * @param redirectAttributes Attributes để truyền message qua redirect
     * @return Redirect về trang forgot-password với message
     */
    @PostMapping("/forgot-password")
    public String processForgotPassword(
            @RequestParam String email,
            RedirectAttributes redirectAttributes) {
        
        log.info("Processing forgot password request for email: {}", email);
        
        try {
            passwordResetService.createPasswordResetToken(email);
            redirectAttributes.addFlashAttribute("message", 
                "Nếu email tồn tại trong hệ thống, bạn sẽ nhận được email hướng dẫn reset mật khẩu trong vài phút");
            redirectAttributes.addFlashAttribute("messageType", "success");
            
        } catch (IllegalStateException e) {
            // Rate limit exceeded
            log.warn("Rate limit exceeded for email: {}", email);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        } catch (Exception e) {
            log.error("Error processing forgot password request for email: {}", email, e);
            redirectAttributes.addFlashAttribute("error", "Đã xảy ra lỗi. Vui lòng thử lại sau");
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        
        return "redirect:/forgot-password";
    }
    
    /**
     * Hiển thị form reset password
     * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
     * 
     * @param token Token reset password từ email
     * @param model Model để truyền dữ liệu sang view
     * @return View reset-password hoặc reset-password-error
     */
    @GetMapping("/reset-password")
    public String showResetPasswordForm(
            @RequestParam String token,
            Model model) {
        
        log.info("Displaying reset password form for token");
        
        try {
            // Validate token
            passwordResetService.validateToken(token);
            model.addAttribute("token", token);
            return "register/reset-password";
            
        } catch (Exception e) {
            log.warn("Invalid or expired token attempted: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "register/reset-password-error";
        }
    }
    
    /**
     * Xử lý reset password
     * Requirements: 4.1, 4.10, 4.11, 4.12, 4.13, 4.14, 4.15, 4.16, 5.14, 5.15
     * 
     * @param token Token reset password
     * @param password Mật khẩu mới
     * @param confirmPassword Xác nhận mật khẩu mới
     * @param redirectAttributes Attributes để truyền message qua redirect
     * @return Redirect về login hoặc reset-password
     */
    @PostMapping("/reset-password")
    public String processResetPassword(
            @RequestParam String token,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {
        
        log.info("Processing reset password request");
        
        // Validate passwords match
        if (!password.equals(confirmPassword)) {
            log.warn("Password confirmation mismatch");
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp");
            return "redirect:/reset-password?token=" + token;
        }
        
        try {
            passwordResetService.resetPassword(token, password);
            redirectAttributes.addFlashAttribute("message", 
                "Mật khẩu đã được đặt lại thành công. Vui lòng đăng nhập");
            redirectAttributes.addFlashAttribute("messageType", "success");
            log.info("Password reset successfully");
            return "redirect:/login";
            
        } catch (Exception e) {
            log.error("Error resetting password: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/reset-password?token=" + token;
        }
    }
}
