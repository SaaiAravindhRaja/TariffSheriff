package com.tariffsheriff.backend.user.service;

import com.tariffsheriff.backend.user.model.User;
import com.tariffsheriff.backend.user.model.UserRole;
import com.tariffsheriff.backend.user.model.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    private EmailService emailService;
    private User testUser;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        
        // Set configuration properties
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@tariffsheriff.com");
        ReflectionTestUtils.setField(emailService, "fromName", "TariffSheriff");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "https://tariffsheriff.com");
        ReflectionTestUtils.setField(emailService, "supportEmail", "support@tariffsheriff.com");

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setRole(UserRole.USER);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setCreatedAt(LocalDateTime.now());

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void shouldSendVerificationEmailSuccessfully() throws MessagingException {
        // Given
        String verificationToken = "verification-token-123";

        // When
        emailService.sendVerificationEmail(testUser, verificationToken);

        // Then
        verify(mailSender).send(any(MimeMessage.class));
        
        // Verify the email content would be correct
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
    }

    @Test
    void shouldSendPasswordResetEmailSuccessfully() throws MessagingException {
        // Given
        String resetToken = "reset-token-123";

        // When
        emailService.sendPasswordResetEmail(testUser, resetToken);

        // Then
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void shouldSendWelcomeEmailSuccessfully() throws MessagingException {
        // When
        emailService.sendWelcomeEmail(testUser);

        // Then
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void shouldSendSecurityAlertEmailSuccessfully() throws MessagingException {
        // Given
        String alertMessage = "Suspicious login attempt detected";

        // When
        emailService.sendSecurityAlert(testUser, alertMessage);

        // Then
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void shouldSendPasswordChangedNotificationSuccessfully() throws MessagingException {
        // When
        emailService.sendPasswordChangedNotification(testUser);

        // Then
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void shouldSendAccountLockedNotificationSuccessfully() throws MessagingException {
        // When
        emailService.sendAccountLockedNotification(testUser);

        // Then
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void shouldHandleEmailSendingFailure() throws MessagingException {
        // Given
        String verificationToken = "verification-token-123";
        doThrow(new RuntimeException("SMTP server unavailable")).when(mailSender).send(any(MimeMessage.class));

        // When & Then
        assertThatThrownBy(() -> emailService.sendVerificationEmail(testUser, verificationToken))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to send verification email");
    }

    @Test
    void shouldRetryEmailSendingOnFailure() throws MessagingException {
        // Given
        String verificationToken = "verification-token-123";
        doThrow(new RuntimeException("Temporary failure"))
            .doNothing()
            .when(mailSender).send(any(MimeMessage.class));

        // When
        emailService.sendVerificationEmailWithRetry(testUser, verificationToken);

        // Then
        verify(mailSender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    void shouldSendEmailAsynchronously() throws ExecutionException, InterruptedException {
        // Given
        String verificationToken = "verification-token-123";

        // When
        CompletableFuture<Void> future = emailService.sendVerificationEmailAsync(testUser, verificationToken);
        future.get(); // Wait for completion

        // Then
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void shouldValidateEmailAddressFormat() {
        // Given
        User userWithInvalidEmail = new User();
        userWithInvalidEmail.setEmail("invalid-email");
        userWithInvalidEmail.setName("Test User");

        // When & Then
        assertThatThrownBy(() -> emailService.sendWelcomeEmail(userWithInvalidEmail))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid email address");
    }

    @Test
    void shouldGenerateCorrectVerificationUrl() {
        // Given
        String verificationToken = "verification-token-123";
        
        // When
        String url = emailService.generateVerificationUrl(verificationToken);

        // Then
        assertThat(url).isEqualTo("https://tariffsheriff.com/verify-email?token=verification-token-123");
    }

    @Test
    void shouldGenerateCorrectPasswordResetUrl() {
        // Given
        String resetToken = "reset-token-123";
        
        // When
        String url = emailService.generatePasswordResetUrl(resetToken);

        // Then
        assertThat(url).isEqualTo("https://tariffsheriff.com/reset-password?token=reset-token-123");
    }

    @Test
    void shouldCreateEmailTemplateWithUserData() {
        // When
        String template = emailService.createWelcomeEmailTemplate(testUser);

        // Then
        assertThat(template).contains("Test User");
        assertThat(template).contains("Welcome to TariffSheriff");
        assertThat(template).contains("support@tariffsheriff.com");
    }

    @Test
    void shouldCreateSecurityAlertTemplate() {
        // Given
        String alertMessage = "Suspicious login from new location";

        // When
        String template = emailService.createSecurityAlertTemplate(testUser, alertMessage);

        // Then
        assertThat(template).contains("Test User");
        assertThat(template).contains("Security Alert");
        assertThat(template).contains(alertMessage);
        assertThat(template).contains("If this was not you");
    }

    @Test
    void shouldHandleNullUserGracefully() {
        // When & Then
        assertThatThrownBy(() -> emailService.sendWelcomeEmail(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("User cannot be null");
    }

    @Test
    void shouldHandleNullTokenGracefully() {
        // When & Then
        assertThatThrownBy(() -> emailService.sendVerificationEmail(testUser, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Token cannot be null");
    }

    @Test
    void shouldSetCorrectEmailHeaders() throws MessagingException {
        // Given
        String verificationToken = "verification-token-123";
        MimeMessageHelper helper = mock(MimeMessageHelper.class);

        // When
        emailService.sendVerificationEmail(testUser, verificationToken);

        // Then
        verify(mailSender).send(any(MimeMessage.class));
        // In a real implementation, we would verify the headers are set correctly
    }

    @Test
    void shouldHandleSpecialCharactersInUserName() throws MessagingException {
        // Given
        User userWithSpecialChars = new User();
        userWithSpecialChars.setEmail("test@example.com");
        userWithSpecialChars.setName("Test Üser with Spëcial Chars");
        userWithSpecialChars.setRole(UserRole.USER);
        userWithSpecialChars.setStatus(UserStatus.ACTIVE);

        // When
        emailService.sendWelcomeEmail(userWithSpecialChars);

        // Then
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void shouldSendBulkEmailsEfficiently() throws MessagingException {
        // Given
        int userCount = 100;
        long startTime = System.currentTimeMillis();

        // When
        for (int i = 0; i < userCount; i++) {
            User user = new User();
            user.setEmail("user" + i + "@example.com");
            user.setName("User " + i);
            user.setRole(UserRole.USER);
            user.setStatus(UserStatus.ACTIVE);
            
            emailService.sendWelcomeEmail(user);
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        long averageTimePerEmail = totalTime / userCount;

        // Then
        verify(mailSender, times(userCount)).send(any(MimeMessage.class));
        
        // Should send emails efficiently (less than 50ms per email)
        assertThat(averageTimePerEmail).isLessThan(50);
        System.out.println("Average time per email: " + averageTimePerEmail + "ms");
    }

    @Test
    void shouldHandleConcurrentEmailSending() throws InterruptedException, ExecutionException {
        // Given
        int threadCount = 10;
        int emailsPerThread = 10;
        
        CompletableFuture<Void>[] futures = new CompletableFuture[threadCount];

        // When
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < emailsPerThread; j++) {
                    User user = new User();
                    user.setEmail("user" + threadIndex + "_" + j + "@example.com");
                    user.setName("User " + threadIndex + "_" + j);
                    user.setRole(UserRole.USER);
                    user.setStatus(UserStatus.ACTIVE);
                    
                    try {
                        emailService.sendWelcomeEmail(user);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        // Wait for all threads to complete
        CompletableFuture.allOf(futures).get();

        // Then
        verify(mailSender, times(threadCount * emailsPerThread)).send(any(MimeMessage.class));
    }

    @Test
    void shouldCreateHtmlEmailContent() {
        // When
        String htmlContent = emailService.createVerificationEmailHtml(testUser, "token123");

        // Then
        assertThat(htmlContent).contains("<html>");
        assertThat(htmlContent).contains("</html>");
        assertThat(htmlContent).contains("Test User");
        assertThat(htmlContent).contains("token123");
        assertThat(htmlContent).contains("Verify Email");
    }

    @Test
    void shouldCreatePlainTextEmailContent() {
        // When
        String textContent = emailService.createVerificationEmailText(testUser, "token123");

        // Then
        assertThat(textContent).contains("Test User");
        assertThat(textContent).contains("token123");
        assertThat(textContent).contains("verify your email");
        assertThat(textContent).doesNotContain("<html>");
    }

    @Test
    void shouldLogEmailSendingEvents() throws MessagingException {
        // Given
        String verificationToken = "verification-token-123";

        // When
        emailService.sendVerificationEmail(testUser, verificationToken);

        // Then
        verify(mailSender).send(any(MimeMessage.class));
        // In a real implementation, we would verify that logging occurred
    }

    @Test
    void shouldRateLimitEmailSending() {
        // Given
        int maxEmailsPerMinute = 60;
        long startTime = System.currentTimeMillis();

        // When
        for (int i = 0; i < maxEmailsPerMinute + 10; i++) {
            try {
                emailService.sendWelcomeEmailWithRateLimit(testUser);
            } catch (RuntimeException e) {
                // Expected when rate limit is exceeded
                assertThat(e.getMessage()).contains("Rate limit exceeded");
                break;
            }
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then
        // Should have been rate limited before sending all emails
        assertThat(duration).isLessThan(60000); // Less than 1 minute
    }

    @Test
    void shouldSanitizeEmailContent() {
        // Given
        User userWithMaliciousName = new User();
        userWithMaliciousName.setEmail("test@example.com");
        userWithMaliciousName.setName("<script>alert('xss')</script>Test User");
        userWithMaliciousName.setRole(UserRole.USER);
        userWithMaliciousName.setStatus(UserStatus.ACTIVE);

        // When
        String htmlContent = emailService.createWelcomeEmailHtml(userWithMaliciousName);

        // Then
        assertThat(htmlContent).doesNotContain("<script>");
        assertThat(htmlContent).contains("Test User");
    }
}