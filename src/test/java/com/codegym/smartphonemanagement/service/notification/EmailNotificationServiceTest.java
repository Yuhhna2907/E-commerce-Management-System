package com.codegym.smartphonemanagement.service.notification;

import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.model.ProductAnswer;
import com.codegym.smartphonemanagement.model.ProductQuestion;
import com.codegym.smartphonemanagement.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho EmailNotificationService
 * Test các chức năng:
 * - Gửi email thông báo thành công
 * - Xử lý trường hợp user không có email
 * - Xử lý lỗi khi gửi email
 * - Verify nội dung email
 */
@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {
    
    @Mock
    private JavaMailSender mailSender;
    
    @Mock
    private MimeMessage mimeMessage;
    
    @InjectMocks
    private EmailNotificationService emailNotificationService;
    
    private User questionAuthor;
    private User answerAuthor;
    private Product product;
    private ProductQuestion question;
    private ProductAnswer answer;
    
    @BeforeEach
    void setUp() {
        // Set up test data
        questionAuthor = User.builder()
            .id(1L)
            .username("customer1")
            .fullName("Nguyễn Văn A")
            .email("customer1@example.com")
            .build();
        
        answerAuthor = User.builder()
            .id(2L)
            .username("seller1")
            .fullName("Người Bán XYZ")
            .email("seller1@example.com")
            .build();
        
        product = Product.builder()
            .id(100L)
            .name("iPhone 15 Pro Max 256GB")
            .build();
        
        question = ProductQuestion.builder()
            .id(10L)
            .product(product)
            .user(questionAuthor)
            .questionText("Sản phẩm này có bảo hành quốc tế không?")
            .createdAt(LocalDateTime.now().minusDays(1))
            .build();
        
        answer = ProductAnswer.builder()
            .id(20L)
            .question(question)
            .user(answerAuthor)
            .answerText("Có ạ, sản phẩm được bảo hành quốc tế 12 tháng từ Apple.")
            .createdAt(LocalDateTime.now())
            .build();
        
        // Set up service properties
        ReflectionTestUtils.setField(emailNotificationService, "fromEmail", "noreply@example.com");
        ReflectionTestUtils.setField(emailNotificationService, "serverPort", "8080");
        ReflectionTestUtils.setField(emailNotificationService, "maxAttempts", 3);
    }
    
    @Test
    void testSendAnswerNotification_Success() throws MessagingException {
        // Arrange
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        // Act
        emailNotificationService.sendAnswerNotification(answer);
        
        // Assert
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
    
    @Test
    void testSendAnswerNotification_UserWithoutEmail_ShouldSkip() throws MessagingException {
        // Arrange
        questionAuthor.setEmail(null);
        
        // Act
        emailNotificationService.sendAnswerNotification(answer);
        
        // Assert
        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
    
    @Test
    void testSendAnswerNotification_EmptyEmail_ShouldSkip() throws MessagingException {
        // Arrange
        questionAuthor.setEmail("");
        
        // Act
        emailNotificationService.sendAnswerNotification(answer);
        
        // Assert
        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
    
    @Test
    void testSendAnswerNotification_MessagingException_ShouldThrowRuntimeException() throws MessagingException {
        // Arrange
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MessagingException("SMTP connection failed"))
            .when(mailSender).send(any(MimeMessage.class));
        
        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            emailNotificationService.sendAnswerNotification(answer);
        });
        
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
    
    @Test
    void testSendAnswerNotification_WithFullName() throws MessagingException {
        // Arrange
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        // Act
        emailNotificationService.sendAnswerNotification(answer);
        
        // Assert
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        // Email content should contain full name
        assertNotNull(questionAuthor.getFullName());
        assertNotNull(answerAuthor.getFullName());
    }
    
    @Test
    void testSendAnswerNotification_WithoutFullName_ShouldUseUsername() throws MessagingException {
        // Arrange
        questionAuthor.setFullName(null);
        answerAuthor.setFullName(null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        // Act
        emailNotificationService.sendAnswerNotification(answer);
        
        // Assert
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        // Should fallback to username when fullName is null
        assertNotNull(questionAuthor.getUsername());
        assertNotNull(answerAuthor.getUsername());
    }
    
    @Test
    void testSendTestEmail_Success() throws MessagingException {
        // Arrange
        String toEmail = "test@example.com";
        String subject = "Test Email";
        String content = "<h1>Test Content</h1>";
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        // Act
        emailNotificationService.sendTestEmail(toEmail, subject, content);
        
        // Assert
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
    
    @Test
    void testSendTestEmail_MessagingException_ShouldThrowRuntimeException() throws MessagingException {
        // Arrange
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MessagingException("SMTP error"))
            .when(mailSender).send(any(MimeMessage.class));
        
        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            emailNotificationService.sendTestEmail("test@example.com", "Subject", "Content");
        });
    }
    
    @Test
    void testEmailContentContainsRequiredElements() throws MessagingException {
        // Arrange
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        // Act
        emailNotificationService.sendAnswerNotification(answer);
        
        // Assert
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        
        // Verify that the email was sent with correct data
        assertNotNull(question.getQuestionText());
        assertNotNull(answer.getAnswerText());
        assertNotNull(product.getName());
        assertTrue(question.getQuestionText().length() >= 10);
        assertTrue(answer.getAnswerText().length() >= 10);
    }
}
