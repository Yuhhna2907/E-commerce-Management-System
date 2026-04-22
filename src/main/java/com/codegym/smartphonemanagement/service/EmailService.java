package com.codegym.smartphonemanagement.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service xử lý gửi email cho các chức năng của hệ thống
 * Sử dụng @Async để không block main thread khi gửi email
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    /**
     * Gửi email reset password
     * 
     * @param to Email người nhận
     * @param userName Tên người dùng
     * @param resetLink Link reset password
     */
    @Async
    public void sendPasswordResetEmail(String to, String userName, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject("Yêu cầu đặt lại mật khẩu - SmartZone");
            helper.setFrom("noreply@smartzone.com");
            
            // Tạo context cho Thymeleaf template
            Context context = new Context();
            context.setVariable("userName", userName != null ? userName : "Xin chào");
            context.setVariable("resetLink", resetLink);
            context.setVariable("expiryHours", 1);
            
            // Render HTML content từ template
            String htmlContent = templateEngine.process("email/password-reset", context);
            helper.setText(htmlContent, true);
            
            // Gửi email
            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", to);
            
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}. Error: {}", to, e.getMessage(), e);
            // Không throw exception để không làm gián đoạn flow
            // User vẫn nhận được thông báo thành công (security best practice)
        } catch (Exception e) {
            log.error("Unexpected error while sending password reset email to: {}. Error: {}", to, e.getMessage(), e);
        }
    }
    
    /**
     * Gửi email xác nhận password đã thay đổi thành công
     * 
     * @param to Email người nhận
     * @param userName Tên người dùng
     */
    @Async
    public void sendPasswordChangedEmail(String to, String userName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject("Mật khẩu đã được thay đổi - SmartZone");
            helper.setFrom("noreply@smartzone.com");
            
            // Tạo context cho Thymeleaf template
            Context context = new Context();
            context.setVariable("userName", userName != null ? userName : "Xin chào");
            
            // Format thời gian thay đổi
            LocalDateTime changeTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            context.setVariable("changeTime", changeTime.format(formatter));
            
            // Render HTML content từ template
            String htmlContent = templateEngine.process("email/password-changed", context);
            helper.setText(htmlContent, true);
            
            // Gửi email
            mailSender.send(message);
            log.info("Password changed confirmation email sent successfully to: {}", to);
            
        } catch (MessagingException e) {
            log.error("Failed to send password changed email to: {}. Error: {}", to, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error while sending password changed email to: {}. Error: {}", to, e.getMessage(), e);
        }
    }

    /**
     * Gửi email thông báo duyệt hoàn tiền
     */
    @Async
    public void sendRefundApprovedEmail(String to, String userName, Long orderId, String amount, String adminNote) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("✅ Yêu cầu hoàn tiền đã được duyệt - Đơn #" + orderId + " - SmartZone");
            helper.setFrom("noreply@smartzone.com");

            Context context = new Context();
            context.setVariable("userName", userName != null ? userName : "Quý khách");
            context.setVariable("orderId", orderId);
            context.setVariable("amount", amount);
            context.setVariable("adminNote", adminNote);
            context.setVariable("approved", true);

            String htmlContent = templateEngine.process("email/refund-notification", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Refund APPROVED email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send refund approved email to: {}. Error: {}", to, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending refund approved email to: {}", to, e);
        }
    }

    /**
     * Gửi email thông báo từ chối hoàn tiền
     */
    @Async
    public void sendRefundRejectedEmail(String to, String userName, Long orderId, String amount, String adminNote) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("❌ Yêu cầu hoàn tiền đã bị từ chối - Đơn #" + orderId + " - SmartZone");
            helper.setFrom("noreply@smartzone.com");

            Context context = new Context();
            context.setVariable("userName", userName != null ? userName : "Quý khách");
            context.setVariable("orderId", orderId);
            context.setVariable("amount", amount);
            context.setVariable("adminNote", adminNote);
            context.setVariable("approved", false);

            String htmlContent = templateEngine.process("email/refund-notification", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Refund REJECTED email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send refund rejected email to: {}. Error: {}", to, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending refund rejected email to: {}", to, e);
        }
    }

    /**
     * Gửi email thông báo khi Admin trả lời câu hỏi sản phẩm
     * 
     * @param to Email người nhận
     * @param userName Tên người dùng
     * @param productName Tên sản phẩm
     * @param questionText Nội dung câu hỏi
     * @param answerText Nội dung câu trả lời
     */
    @Async
    public void sendAnswerNotificationEmail(String to, String userName, String productName, String questionText, String answerText) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("💡 Chào " + userName + ", câu hỏi của bạn đã được giải đáp! - SmartZone");
            helper.setFrom("noreply@smartzone.com");

            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("productName", productName);
            context.setVariable("questionText", questionText);
            context.setVariable("answerText", answerText);
            context.setVariable("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            String htmlContent = templateEngine.process("email/answer-notification", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Q&A answer notification email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send Q&A notification email to: {}. Error: {}", to, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while sending Q&A notification email to: {}", to, e);
        }
    }
    /**
     * Gửi email khuyến mãi/thông báo hàng loạt
     * 
     * @param to Email người nhận
     * @param userName Tên người dùng
     * @param subject Chủ đề email
     * @param content Nội dung chi tiết
     * @param actionLink Link đính kèm
     */
    @Async("broadcastExecutor")
    public void sendPromotionalEmail(String to, String userName, String subject, String content, String actionLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("marketing@smartzone.com");

            Context context = new Context();
            context.setVariable("userName", userName != null ? userName : "User");
            context.setVariable("content", content);
            context.setVariable("actionLink", actionLink);

            String htmlContent = templateEngine.process("email/promotion", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            // We use trace/debug here to avoid log spamming for large broadcasts
            log.debug("Promotional email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send promotional email to: {}. Error: {}", to, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while sending promotional email to: {}", to, e);
        }
    }
}
