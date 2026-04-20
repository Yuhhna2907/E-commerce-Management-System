package com.codegym.smartphonemanagement.service.notification;

import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.model.ProductAnswer;
import com.codegym.smartphonemanagement.model.ProductQuestion;
import com.codegym.smartphonemanagement.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * Service xử lý gửi email thông báo
 * Sử dụng @Async để gửi email không đồng bộ
 * Sử dụng @Retryable để retry khi gửi thất bại
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.email.retry.max-attempts:3}")
    private int maxAttempts;
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    /**
     * Gửi email thông báo khi câu hỏi được trả lời
     * 
     * @param answer Câu trả lời vừa được tạo
     * @throws MessagingException Nếu có lỗi khi gửi email
     */
    @Async
    @Retryable(
        retryFor = {MessagingException.class, Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(
            delay = 1000,      // Initial delay: 1 second
            multiplier = 2.0,  // Exponential backoff multiplier
            maxDelay = 10000   // Max delay: 10 seconds
        )
    )
    public void sendAnswerNotification(ProductAnswer answer) {
        try {
            ProductQuestion question = answer.getQuestion();
            User questionAuthor = question.getUser();
            Product product = question.getProduct();
            User answerAuthor = answer.getUser();
            
            // Validate email
            if (questionAuthor.getEmail() == null || questionAuthor.getEmail().isEmpty()) {
                log.warn("User {} không có email, bỏ qua gửi thông báo", questionAuthor.getId());
                return;
            }
            
            // Tạo email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(questionAuthor.getEmail());
            helper.setSubject("Câu hỏi của bạn đã được trả lời - " + product.getName());
            
            // Tạo nội dung email HTML
            String emailContent = buildAnswerNotificationEmailContent(
                questionAuthor,
                question,
                answer,
                answerAuthor,
                product
            );
            
            helper.setText(emailContent, true);
            
            // Gửi email
            mailSender.send(message);
            
            log.info("Đã gửi email thông báo đến {} cho câu hỏi ID: {}", 
                questionAuthor.getEmail(), question.getId());
            
        } catch (MessagingException e) {
            log.error("Lỗi khi gửi email thông báo cho câu hỏi ID: {}, lỗi: {}", 
                answer.getQuestion().getId(), e.getMessage(), e);
            throw new RuntimeException("Không thể gửi email thông báo", e);
        }
    }
    
    /**
     * Xây dựng nội dung email HTML
     */
    private String buildAnswerNotificationEmailContent(
            User questionAuthor,
            ProductQuestion question,
            ProductAnswer answer,
            User answerAuthor,
            Product product) {
        
        String productUrl = String.format("http://localhost:%s/user/products/%d", 
            serverPort, product.getId());
        
        String answerDate = answer.getCreatedAt().format(DATE_FORMATTER);
        
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                        background-color: #f4f4f4;
                    }
                    .email-container {
                        background-color: #ffffff;
                        border-radius: 8px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        overflow: hidden;
                    }
                    .email-header {
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                        padding: 30px 20px;
                        text-align: center;
                    }
                    .email-header h1 {
                        margin: 0;
                        font-size: 24px;
                        font-weight: 600;
                    }
                    .email-body {
                        padding: 30px 20px;
                    }
                    .greeting {
                        font-size: 16px;
                        margin-bottom: 20px;
                        color: #555;
                    }
                    .product-info {
                        background-color: #f8f9fa;
                        border-left: 4px solid #667eea;
                        padding: 15px;
                        margin: 20px 0;
                        border-radius: 4px;
                    }
                    .product-name {
                        font-weight: 600;
                        color: #667eea;
                        font-size: 18px;
                        margin-bottom: 10px;
                    }
                    .question-section, .answer-section {
                        margin: 20px 0;
                        padding: 15px;
                        border-radius: 4px;
                    }
                    .question-section {
                        background-color: #fff3cd;
                        border-left: 4px solid #ffc107;
                    }
                    .answer-section {
                        background-color: #d1ecf1;
                        border-left: 4px solid #17a2b8;
                    }
                    .section-title {
                        font-weight: 600;
                        margin-bottom: 10px;
                        font-size: 14px;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                    }
                    .question-text, .answer-text {
                        color: #333;
                        line-height: 1.6;
                        margin: 10px 0;
                    }
                    .author-info {
                        font-size: 13px;
                        color: #666;
                        margin-top: 10px;
                        font-style: italic;
                    }
                    .cta-button {
                        display: inline-block;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                        text-decoration: none;
                        padding: 14px 30px;
                        border-radius: 25px;
                        font-weight: 600;
                        margin: 20px 0;
                        text-align: center;
                        transition: transform 0.2s;
                    }
                    .cta-button:hover {
                        transform: translateY(-2px);
                    }
                    .email-footer {
                        background-color: #f8f9fa;
                        padding: 20px;
                        text-align: center;
                        font-size: 13px;
                        color: #666;
                        border-top: 1px solid #e9ecef;
                    }
                    .divider {
                        height: 1px;
                        background-color: #e9ecef;
                        margin: 20px 0;
                    }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="email-header">
                        <h1>🎉 Câu hỏi của bạn đã được trả lời!</h1>
                    </div>
                    
                    <div class="email-body">
                        <p class="greeting">Xin chào <strong>%s</strong>,</p>
                        
                        <p>Chúng tôi rất vui thông báo rằng câu hỏi của bạn về sản phẩm đã nhận được câu trả lời từ người bán.</p>
                        
                        <div class="product-info">
                            <div class="product-name">📦 %s</div>
                        </div>
                        
                        <div class="question-section">
                            <div class="section-title">❓ Câu hỏi của bạn:</div>
                            <div class="question-text">"%s"</div>
                        </div>
                        
                        <div class="answer-section">
                            <div class="section-title">💬 Câu trả lời:</div>
                            <div class="answer-text">%s</div>
                            <div class="author-info">
                                Trả lời bởi: <strong>%s</strong> vào %s
                            </div>
                        </div>
                        
                        <div style="text-align: center;">
                            <a href="%s" class="cta-button">
                                Xem chi tiết sản phẩm →
                            </a>
                        </div>
                        
                        <div class="divider"></div>
                        
                        <p style="font-size: 14px; color: #666;">
                            Nếu bạn có thêm câu hỏi, đừng ngần ngại đặt câu hỏi mới trên trang sản phẩm.
                        </p>
                    </div>
                    
                    <div class="email-footer">
                        <p>Email này được gửi tự động từ hệ thống Smartphone Management.</p>
                        <p>Vui lòng không trả lời email này.</p>
                        <p style="margin-top: 10px;">
                            © 2026 Smartphone Management. All rights reserved.
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                questionAuthor.getFullName() != null ? questionAuthor.getFullName() : questionAuthor.getUsername(),
                product.getName(),
                question.getQuestionText(),
                answer.getAnswerText(),
                answerAuthor.getFullName() != null ? answerAuthor.getFullName() : answerAuthor.getUsername(),
                answerDate,
                productUrl
            );
    }
    
    /**
     * Gửi email test (dùng cho testing)
     */
    @Async
    public void sendTestEmail(String toEmail, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true);
            
            mailSender.send(message);
            
            log.info("Đã gửi test email đến {}", toEmail);
            
        } catch (MessagingException e) {
            log.error("Lỗi khi gửi test email: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể gửi test email", e);
        }
    }
}
