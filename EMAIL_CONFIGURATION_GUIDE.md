# Hướng Dẫn Cấu Hình JavaMailSender Với Nhiều Email Account

## Tổng Quan

Tài liệu này hướng dẫn cách cấu hình Spring Boot để sử dụng nhiều email account khác nhau cho các mục đích khác nhau (ví dụ: noreply@, support@, marketing@).

## Phương Án 1: Multiple JavaMailSender Beans (Recommended)

### 1. Cấu hình trong application.properties

```properties
# ===== EMAIL CONFIGURATION - MULTIPLE ACCOUNTS =====

# Primary Email (No-Reply) - Dùng cho password reset, notifications
spring.mail.primary.host=smtp.gmail.com
spring.mail.primary.port=587
spring.mail.primary.username=noreply@smartzone.com
spring.mail.primary.password=${EMAIL_NOREPLY_PASSWORD}
spring.mail.primary.from=noreply@smartzone.com
spring.mail.primary.from-name=SmartZone

# Support Email - Dùng cho customer support
spring.mail.support.host=smtp.gmail.com
spring.mail.support.port=587
spring.mail.support.username=support@smartzone.com
spring.mail.support.password=${EMAIL_SUPPORT_PASSWORD}
spring.mail.support.from=support@smartzone.com
spring.mail.support.from-name=SmartZone Support

# Marketing Email - Dùng cho promotional emails
spring.mail.marketing.host=smtp.gmail.com
spring.mail.marketing.port=587
spring.mail.marketing.username=marketing@smartzone.com
spring.mail.marketing.password=${EMAIL_MARKETING_PASSWORD}
spring.mail.marketing.from=marketing@smartzone.com
spring.mail.marketing.from-name=SmartZone Marketing

# Common SMTP Properties
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000

# Base URL
app.base-url=http://localhost:8080
```

### 2. Tạo Configuration Class

**File**: `config/MailConfig.java`

```java
package com.codegym.smartphonemanagement.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    /**
     * Primary JavaMailSender - Dùng cho password reset, system notifications
     */
    @Bean(name = "primaryMailSender")
    @Primary
    @ConfigurationProperties(prefix = "spring.mail.primary")
    public JavaMailSender primaryMailSender() {
        return createMailSender();
    }

    /**
     * Support JavaMailSender - Dùng cho customer support emails
     */
    @Bean(name = "supportMailSender")
    @ConfigurationProperties(prefix = "spring.mail.support")
    public JavaMailSender supportMailSender() {
        return createMailSender();
    }

    /**
     * Marketing JavaMailSender - Dùng cho promotional emails
     */
    @Bean(name = "marketingMailSender")
    @ConfigurationProperties(prefix = "spring.mail.marketing")
    public JavaMailSender marketingMailSender() {
        return createMailSender();
    }

    /**
     * Helper method để tạo JavaMailSender với common properties
     */
    private JavaMailSenderImpl createMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        
        return mailSender;
    }

    /**
     * Configuration properties cho Primary Mail
     */
    @Data
    @ConfigurationProperties(prefix = "spring.mail.primary")
    public static class PrimaryMailProperties {
        private String host;
        private int port;
        private String username;
        private String password;
        private String from;
        private String fromName;
    }

    /**
     * Configuration properties cho Support Mail
     */
    @Data
    @ConfigurationProperties(prefix = "spring.mail.support")
    public static class SupportMailProperties {
        private String host;
        private int port;
        private String username;
        private String password;
        private String from;
        private String fromName;
    }

    /**
     * Configuration properties cho Marketing Mail
     */
    @Data
    @ConfigurationProperties(prefix = "spring.mail.marketing")
    public static class MarketingMailProperties {
        private String host;
        private int port;
        private String username;
        private String password;
        private String from;
        private String fromName;
    }
}
```

### 3. Update EmailService để sử dụng nhiều senders

**File**: `service/EmailService.java`

```java
package com.codegym.smartphonemanagement.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@Slf4j
public class EmailService {
    
    private final JavaMailSender primaryMailSender;
    private final JavaMailSender supportMailSender;
    private final JavaMailSender marketingMailSender;
    private final TemplateEngine templateEngine;
    
    @Value("${spring.mail.primary.from}")
    private String primaryFrom;
    
    @Value("${spring.mail.primary.from-name}")
    private String primaryFromName;
    
    @Value("${spring.mail.support.from}")
    private String supportFrom;
    
    @Value("${spring.mail.support.from-name}")
    private String supportFromName;
    
    @Value("${spring.mail.marketing.from}")
    private String marketingFrom;
    
    @Value("${spring.mail.marketing.from-name}")
    private String marketingFromName;
    
    public EmailService(
            @Qualifier("primaryMailSender") JavaMailSender primaryMailSender,
            @Qualifier("supportMailSender") JavaMailSender supportMailSender,
            @Qualifier("marketingMailSender") JavaMailSender marketingMailSender,
            TemplateEngine templateEngine) {
        this.primaryMailSender = primaryMailSender;
        this.supportMailSender = supportMailSender;
        this.marketingMailSender = marketingMailSender;
        this.templateEngine = templateEngine;
    }
    
    /**
     * Gửi email reset password (sử dụng primary/noreply account)
     */
    @Async
    public void sendPasswordResetEmail(String to, String userName, String resetLink) {
        try {
            MimeMessage message = primaryMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(primaryFrom, primaryFromName);
            helper.setTo(to);
            helper.setSubject("Yêu cầu đặt lại mật khẩu - SmartZone");
            
            Context context = new Context();
            context.setVariable("userName", userName != null ? userName : "Xin chào");
            context.setVariable("resetLink", resetLink);
            
            String htmlContent = templateEngine.process("email/password-reset", context);
            helper.setText(htmlContent, true);
            
            primaryMailSender.send(message);
            log.info("Password reset email sent to: {} from: {}", to, primaryFrom);
            
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {} from: {}", to, primaryFrom, e);
        }
    }
    
    /**
     * Gửi email xác nhận password đã thay đổi (sử dụng primary account)
     */
    @Async
    public void sendPasswordChangedEmail(String to, String userName) {
        try {
            MimeMessage message = primaryMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(primaryFrom, primaryFromName);
            helper.setTo(to);
            helper.setSubject("Mật khẩu đã được thay đổi - SmartZone");
            
            Context context = new Context();
            context.setVariable("userName", userName != null ? userName : "Xin chào");
            context.setVariable("changeTime", java.time.LocalDateTime.now());
            
            String htmlContent = templateEngine.process("email/password-changed", context);
            helper.setText(htmlContent, true);
            
            primaryMailSender.send(message);
            log.info("Password changed confirmation email sent to: {} from: {}", to, primaryFrom);
            
        } catch (Exception e) {
            log.error("Failed to send password changed email to: {} from: {}", to, primaryFrom, e);
        }
    }
    
    /**
     * Gửi email support (sử dụng support account)
     */
    @Async
    public void sendSupportEmail(String to, String subject, String content) {
        try {
            MimeMessage message = supportMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(supportFrom, supportFromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            
            supportMailSender.send(message);
            log.info("Support email sent to: {} from: {}", to, supportFrom);
            
        } catch (Exception e) {
            log.error("Failed to send support email to: {} from: {}", to, supportFrom, e);
        }
    }
    
    /**
     * Gửi email marketing/promotional (sử dụng marketing account)
     */
    @Async
    public void sendMarketingEmail(String to, String subject, String templateName, Context context) {
        try {
            MimeMessage message = marketingMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(marketingFrom, marketingFromName);
            helper.setTo(to);
            helper.setSubject(subject);
            
            String htmlContent = templateEngine.process(templateName, context);
            helper.setText(htmlContent, true);
            
            marketingMailSender.send(message);
            log.info("Marketing email sent to: {} from: {}", to, marketingFrom);
            
        } catch (Exception e) {
            log.error("Failed to send marketing email to: {} from: {}", to, marketingFrom, e);
        }
    }
    
    /**
     * Gửi email order confirmation (sử dụng primary account)
     */
    @Async
    public void sendOrderConfirmationEmail(String to, String orderNumber, Context context) {
        try {
            MimeMessage message = primaryMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(primaryFrom, primaryFromName);
            helper.setTo(to);
            helper.setSubject("Xác nhận đơn hàng #" + orderNumber + " - SmartZone");
            
            String htmlContent = templateEngine.process("email/order-confirmation", context);
            helper.setText(htmlContent, true);
            
            primaryMailSender.send(message);
            log.info("Order confirmation email sent to: {} from: {}", to, primaryFrom);
            
        } catch (Exception e) {
            log.error("Failed to send order confirmation email to: {} from: {}", to, primaryFrom, e);
        }
    }
}
```

## Phương Án 2: Dynamic Email Sender (Alternative)

Nếu bạn muốn linh hoạt hơn, có thể tạo một factory pattern:

```java
@Service
public class EmailSenderFactory {
    
    private final Map<EmailType, JavaMailSender> senders;
    private final Map<EmailType, String> fromAddresses;
    
    public EmailSenderFactory(
            @Qualifier("primaryMailSender") JavaMailSender primaryMailSender,
            @Qualifier("supportMailSender") JavaMailSender supportMailSender,
            @Qualifier("marketingMailSender") JavaMailSender marketingMailSender) {
        
        senders = Map.of(
            EmailType.PRIMARY, primaryMailSender,
            EmailType.SUPPORT, supportMailSender,
            EmailType.MARKETING, marketingMailSender
        );
        
        fromAddresses = Map.of(
            EmailType.PRIMARY, "noreply@smartzone.com",
            EmailType.SUPPORT, "support@smartzone.com",
            EmailType.MARKETING, "marketing@smartzone.com"
        );
    }
    
    public JavaMailSender getSender(EmailType type) {
        return senders.get(type);
    }
    
    public String getFromAddress(EmailType type) {
        return fromAddresses.get(type);
    }
    
    public enum EmailType {
        PRIMARY,
        SUPPORT,
        MARKETING
    }
}
```

## Cấu Hình Gmail App Password

### Bước 1: Bật 2-Step Verification
1. Đăng nhập vào Google Account
2. Vào Security → 2-Step Verification
3. Bật 2-Step Verification

### Bước 2: Tạo App Password
1. Vào Security → App passwords
2. Chọn "Mail" và "Other (Custom name)"
3. Nhập tên: "SmartZone App"
4. Copy password được tạo

### Bước 3: Cấu hình Environment Variables

**Windows (PowerShell)**:
```powershell
$env:EMAIL_NOREPLY_PASSWORD="your-app-password-here"
$env:EMAIL_SUPPORT_PASSWORD="your-app-password-here"
$env:EMAIL_MARKETING_PASSWORD="your-app-password-here"
```

**Linux/Mac**:
```bash
export EMAIL_NOREPLY_PASSWORD="your-app-password-here"
export EMAIL_SUPPORT_PASSWORD="your-app-password-here"
export EMAIL_MARKETING_PASSWORD="your-app-password-here"
```

**IntelliJ IDEA**:
1. Run → Edit Configurations
2. Environment variables:
```
EMAIL_NOREPLY_PASSWORD=your-app-password-here;EMAIL_SUPPORT_PASSWORD=your-app-password-here;EMAIL_MARKETING_PASSWORD=your-app-password-here
```

## Testing

### Test với nhiều email accounts:

```java
@SpringBootTest
class EmailServiceTest {
    
    @Autowired
    private EmailService emailService;
    
    @Test
    void testPasswordResetEmail() {
        emailService.sendPasswordResetEmail(
            "user@example.com",
            "Test User",
            "http://localhost:8080/reset-password?token=abc123"
        );
        // Verify email sent from noreply@smartzone.com
    }
    
    @Test
    void testSupportEmail() {
        emailService.sendSupportEmail(
            "user@example.com",
            "Test Support",
            "<h1>Support Content</h1>"
        );
        // Verify email sent from support@smartzone.com
    }
    
    @Test
    void testMarketingEmail() {
        Context context = new Context();
        context.setVariable("userName", "Test User");
        
        emailService.sendMarketingEmail(
            "user@example.com",
            "Special Offer",
            "email/marketing-template",
            context
        );
        // Verify email sent from marketing@smartzone.com
    }
}
```

## Best Practices

1. **Sử dụng đúng email account cho đúng mục đích**:
   - `noreply@` - System notifications, password reset
   - `support@` - Customer support, help desk
   - `marketing@` - Promotional emails, newsletters

2. **Rate Limiting**: Gmail có giới hạn 500 emails/day cho free account

3. **Error Handling**: Luôn wrap email sending trong try-catch

4. **Async Processing**: Sử dụng @Async để không block main thread

5. **Logging**: Log tất cả email operations để debug

6. **Security**: Không hardcode passwords, sử dụng environment variables

## Troubleshooting

### Lỗi Authentication Failed
- Kiểm tra App Password đúng chưa
- Kiểm tra 2-Step Verification đã bật chưa
- Kiểm tra username (email) đúng chưa

### Lỗi Connection Timeout
- Kiểm tra firewall/antivirus
- Kiểm tra port 587 có bị block không
- Thử port 465 với SSL

### Email không gửi được
- Kiểm tra logs
- Verify SMTP settings
- Test với telnet: `telnet smtp.gmail.com 587`

## Kết Luận

Với cấu hình này, bạn có thể:
- ✅ Sử dụng nhiều email account khác nhau
- ✅ Phân loại email theo mục đích
- ✅ Dễ dàng maintain và scale
- ✅ Professional email management
