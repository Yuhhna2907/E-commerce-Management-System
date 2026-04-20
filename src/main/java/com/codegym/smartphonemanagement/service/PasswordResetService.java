package com.codegym.smartphonemanagement.service;

import com.codegym.smartphonemanagement.model.PasswordResetToken;
import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.repository.PasswordResetTokenRepository;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Service xử lý chức năng reset password
 * Requirements: 3.2, 3.3, 4.1, 5.1, 5.2, 5.6, 5.8
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PasswordResetService {
    
    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    // Constants
    private static final int TOKEN_LENGTH = 64;
    private static final int MAX_REQUESTS_PER_EMAIL = 3;
    private static final int REQUEST_WINDOW_HOURS = 1;
    
    /**
     * Tạo password reset token và gửi email
     * Requirements: 3.2, 3.3, 3.6, 3.7, 3.8, 3.9, 3.13, 3.14, 5.1, 5.2, 5.6, 5.7, 5.8
     * 
     * @param email Email của user yêu cầu reset password
     */
    public void createPasswordResetToken(String email) {
        log.info("Password reset requested for email: {}", email);
        
        // Tìm user theo email (không throw exception nếu không tìm thấy - security)
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        // Luôn trả về thông báo thành công để bảo mật (không tiết lộ email có tồn tại hay không)
        if (userOpt.isEmpty()) {
            log.warn("Password reset requested for non-existent email: {}", email);
            return;
        }
        
        User user = userOpt.get();
        
        // Kiểm tra rate limiting theo email (max 3 requests trong 1 giờ)
        LocalDateTime since = LocalDateTime.now().minusHours(REQUEST_WINDOW_HOURS);
        int recentRequests = tokenRepository.countRecentTokensByEmail(email, since);
        
        if (recentRequests >= MAX_REQUESTS_PER_EMAIL) {
            log.warn("Too many password reset requests for email: {}. Count: {}", email, recentRequests);
            throw new IllegalStateException("Bạn đã yêu cầu reset quá nhiều lần. Vui lòng thử lại sau " + REQUEST_WINDOW_HOURS + " giờ");
        }
        
        // Tạo secure random token với SecureRandom và Base64 encoding
        String token = generateSecureToken();
        
        // Lưu token vào database với expires_at = now + 1 hour
        PasswordResetToken resetToken = PasswordResetToken.builder()
            .user(user)
            .token(token)
            .build();
        
        tokenRepository.save(resetToken);
        log.info("Password reset token created for user ID: {}", user.getId());
        
        // Gửi email reset password
        String resetLink = baseUrl + "/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetLink);
        log.info("Password reset email sent to: {}", user.getEmail());
    }
    
    /**
     * Validate token
     * Requirements: 4.2, 4.3, 4.4, 4.5
     * 
     * @param token Token cần validate
     * @return PasswordResetToken nếu hợp lệ
     * @throws IllegalArgumentException nếu token không tồn tại
     * @throws IllegalStateException nếu token đã sử dụng hoặc hết hạn
     */
    public PasswordResetToken validateToken(String token) {
        log.debug("Validating password reset token");
        
        // Tìm token trong database
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
            .orElseThrow(() -> {
                log.warn("Invalid password reset token attempted");
                return new IllegalArgumentException("Link reset mật khẩu không hợp lệ");
            });
        
        // Kiểm tra token có tồn tại, chưa sử dụng, và chưa hết hạn
        if (resetToken.isUsed()) {
            log.warn("Attempted to use already-used password reset token for user ID: {}", resetToken.getUser().getId());
            throw new IllegalStateException("Link reset mật khẩu đã được sử dụng");
        }
        
        if (resetToken.isExpired()) {
            log.warn("Attempted to use expired password reset token for user ID: {}", resetToken.getUser().getId());
            throw new IllegalStateException("Link reset mật khẩu đã hết hạn. Vui lòng yêu cầu reset mới");
        }
        
        log.debug("Password reset token validated successfully");
        return resetToken;
    }
    
    /**
     * Reset password với token
     * Requirements: 4.13, 4.14, 4.15, 5.14, 5.15
     * 
     * @param token Token reset password
     * @param newPassword Mật khẩu mới
     */
    public void resetPassword(String token, String newPassword) {
        log.info("Processing password reset");
        
        // Validate token bằng validateToken()
        PasswordResetToken resetToken = validateToken(token);
        User user = resetToken.getUser();
        
        // Kiểm tra password mới không trùng password cũ
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            log.warn("User ID {} attempted to reset password with same password", user.getId());
            throw new IllegalArgumentException("Mật khẩu mới không được trùng với mật khẩu cũ");
        }
        
        // Encode password mới với BCrypt
        String encodedPassword = passwordEncoder.encode(newPassword);
        
        // Cập nhật password trong database
        user.setPassword(encodedPassword);
        userRepository.save(user);
        log.info("Password updated successfully for user ID: {}", user.getId());
        
        // Đánh dấu token là đã sử dụng (set used_at)
        resetToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(resetToken);
        log.info("Password reset token marked as used for user ID: {}", user.getId());
        
        // Gửi email xác nhận password đã thay đổi
        emailService.sendPasswordChangedEmail(user.getEmail(), user.getFullName());
        log.info("Password changed confirmation email sent to: {}", user.getEmail());
    }
    
    /**
     * Xóa các tokens đã hết hạn hoặc đã sử dụng sau 24 giờ
     * Requirements: 5.12
     */
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired password reset tokens");
        
        LocalDateTime now = LocalDateTime.now().minusHours(24);
        tokenRepository.deleteExpiredAndUsedTokens(now);
        
        log.info("Completed cleanup of expired password reset tokens");
    }
    
    /**
     * Tạo secure random token với SecureRandom và Base64 encoding
     * Token có độ dài 64 ký tự
     * 
     * @return Secure random token
     */
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[TOKEN_LENGTH];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
