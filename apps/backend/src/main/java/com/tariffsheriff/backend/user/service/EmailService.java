package com.tariffsheriff.backend.user.service;

import com.tariffsheriff.backend.user.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;
    
    @Value("${app.email.from-address:noreply@tariffsheriff.com}")
    private String fromAddress;
    
    @Value("${app.email.from-name:TariffSheriff}")
    private String fromName;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;
    
    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send email verification message
     * Requirements: 11.1, 11.2
     */
    public boolean sendVerificationEmail(User user, String verificationToken) {
        if (!emailEnabled) {
            logger.info("Email sending is disabled. Verification email for {} would be sent.", user.getEmail());
            return true;
        }

        try {
            String verificationUrl = frontendUrl + "/verify-email?token=" + verificationToken;
            
            String subject = "Verify Your TariffSheriff Account";
            String htmlContent = buildVerificationEmailTemplate(user.getName(), verificationUrl);
            String textContent = buildVerificationEmailText(user.getName(), verificationUrl);
            
            sendHtmlEmail(user.getEmail(), subject, htmlContent, textContent);
            
            logger.info("Verification email sent successfully to {}", user.getEmail());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send verification email to {}: {}", user.getEmail(), e.getMessage());
            return false;
        }
    }

    /**
     * Send password reset email
     * Requirements: 11.1, 11.2
     */
    public boolean sendPasswordResetEmail(User user, String resetToken) {
        if (!emailEnabled) {
            logger.info("Email sending is disabled. Password reset email for {} would be sent.", user.getEmail());
            return true;
        }

        try {
            String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;
            
            String subject = "Reset Your TariffSheriff Password";
            String htmlContent = buildPasswordResetEmailTemplate(user.getName(), resetUrl);
            String textContent = buildPasswordResetEmailText(user.getName(), resetUrl);
            
            sendHtmlEmail(user.getEmail(), subject, htmlContent, textContent);
            
            logger.info("Password reset email sent successfully to {}", user.getEmail());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {}: {}", user.getEmail(), e.getMessage());
            return false;
        }
    }

    /**
     * Send security alert email
     * Requirements: 11.3, 11.4
     */
    public boolean sendSecurityAlert(User user, String event, String ipAddress, String userAgent) {
        if (!emailEnabled) {
            logger.info("Email sending is disabled. Security alert for {} would be sent.", user.getEmail());
            return true;
        }

        try {
            String subject = "Security Alert - TariffSheriff Account";
            String htmlContent = buildSecurityAlertEmailTemplate(user.getName(), event, ipAddress, userAgent);
            String textContent = buildSecurityAlertEmailText(user.getName(), event, ipAddress, userAgent);
            
            sendHtmlEmail(user.getEmail(), subject, htmlContent, textContent);
            
            logger.info("Security alert email sent successfully to {}", user.getEmail());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send security alert email to {}: {}", user.getEmail(), e.getMessage());
            return false;
        }
    }

    /**
     * Send welcome email after successful verification
     * Requirements: 11.1, 11.5
     */
    public boolean sendWelcomeEmail(User user) {
        if (!emailEnabled) {
            logger.info("Email sending is disabled. Welcome email for {} would be sent.", user.getEmail());
            return true;
        }

        try {
            String subject = "Welcome to TariffSheriff!";
            String htmlContent = buildWelcomeEmailTemplate(user.getName());
            String textContent = buildWelcomeEmailText(user.getName());
            
            sendHtmlEmail(user.getEmail(), subject, htmlContent, textContent);
            
            logger.info("Welcome email sent successfully to {}", user.getEmail());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send welcome email to {}: {}", user.getEmail(), e.getMessage());
            return false;
        }
    }

    /**
     * Send account settings change notification
     * Requirements: 11.4
     */
    public boolean sendAccountChangeNotification(User user, String changeType) {
        if (!emailEnabled) {
            logger.info("Email sending is disabled. Account change notification for {} would be sent.", user.getEmail());
            return true;
        }

        try {
            String subject = "Account Settings Changed - TariffSheriff";
            String htmlContent = buildAccountChangeEmailTemplate(user.getName(), changeType);
            String textContent = buildAccountChangeEmailText(user.getName(), changeType);
            
            sendHtmlEmail(user.getEmail(), subject, htmlContent, textContent);
            
            logger.info("Account change notification sent successfully to {}", user.getEmail());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send account change notification to {}: {}", user.getEmail(), e.getMessage());
            return false;
        }
    }

    /**
     * Send HTML email with fallback text content
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent, String textContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromAddress, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(textContent, htmlContent);
        
        mailSender.send(message);
    }

    /**
     * Send simple text email
     */
    private void sendTextEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        
        mailSender.send(message);
    }

    // ========== Email Template Methods ==========

    private String buildVerificationEmailTemplate(String userName, String verificationUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Verify Your Account</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #2563eb; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background-color: #f8fafc; padding: 30px; border-radius: 0 0 8px 8px; }
                    .button { display: inline-block; background-color: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; margin: 20px 0; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #e2e8f0; font-size: 14px; color: #64748b; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>TariffSheriff</h1>
                    <p>Verify Your Account</p>
                </div>
                <div class="content">
                    <h2>Hello %s!</h2>
                    <p>Thank you for registering with TariffSheriff. To complete your account setup, please verify your email address by clicking the button below:</p>
                    <p style="text-align: center;">
                        <a href="%s" class="button">Verify Email Address</a>
                    </p>
                    <p>If the button doesn't work, you can copy and paste this link into your browser:</p>
                    <p style="word-break: break-all; background-color: #e2e8f0; padding: 10px; border-radius: 4px;">%s</p>
                    <p><strong>This verification link will expire in 24 hours.</strong></p>
                    <p>If you didn't create an account with TariffSheriff, please ignore this email.</p>
                </div>
                <div class="footer">
                    <p>Best regards,<br>The TariffSheriff Team</p>
                    <p>This is an automated message, please do not reply to this email.</p>
                </div>
            </body>
            </html>
            """.formatted(userName, verificationUrl, verificationUrl);
    }

    private String buildVerificationEmailText(String userName, String verificationUrl) {
        return """
            Hello %s!
            
            Thank you for registering with TariffSheriff. To complete your account setup, please verify your email address by visiting the following link:
            
            %s
            
            This verification link will expire in 24 hours.
            
            If you didn't create an account with TariffSheriff, please ignore this email.
            
            Best regards,
            The TariffSheriff Team
            
            This is an automated message, please do not reply to this email.
            """.formatted(userName, verificationUrl);
    }

    private String buildPasswordResetEmailTemplate(String userName, String resetUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Reset Your Password</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #dc2626; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background-color: #f8fafc; padding: 30px; border-radius: 0 0 8px 8px; }
                    .button { display: inline-block; background-color: #dc2626; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; margin: 20px 0; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #e2e8f0; font-size: 14px; color: #64748b; }
                    .warning { background-color: #fef3c7; border-left: 4px solid #f59e0b; padding: 15px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>TariffSheriff</h1>
                    <p>Password Reset Request</p>
                </div>
                <div class="content">
                    <h2>Hello %s!</h2>
                    <p>We received a request to reset your password for your TariffSheriff account. Click the button below to create a new password:</p>
                    <p style="text-align: center;">
                        <a href="%s" class="button">Reset Password</a>
                    </p>
                    <p>If the button doesn't work, you can copy and paste this link into your browser:</p>
                    <p style="word-break: break-all; background-color: #e2e8f0; padding: 10px; border-radius: 4px;">%s</p>
                    <div class="warning">
                        <p><strong>Important:</strong> This password reset link will expire in 1 hour for security reasons.</p>
                    </div>
                    <p>If you didn't request a password reset, please ignore this email. Your password will remain unchanged.</p>
                </div>
                <div class="footer">
                    <p>Best regards,<br>The TariffSheriff Team</p>
                    <p>This is an automated message, please do not reply to this email.</p>
                </div>
            </body>
            </html>
            """.formatted(userName, resetUrl, resetUrl);
    }

    private String buildPasswordResetEmailText(String userName, String resetUrl) {
        return """
            Hello %s!
            
            We received a request to reset your password for your TariffSheriff account. Visit the following link to create a new password:
            
            %s
            
            IMPORTANT: This password reset link will expire in 1 hour for security reasons.
            
            If you didn't request a password reset, please ignore this email. Your password will remain unchanged.
            
            Best regards,
            The TariffSheriff Team
            
            This is an automated message, please do not reply to this email.
            """.formatted(userName, resetUrl);
    }

    private String buildSecurityAlertEmailTemplate(String userName, String event, String ipAddress, String userAgent) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Security Alert</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #dc2626; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background-color: #f8fafc; padding: 30px; border-radius: 0 0 8px 8px; }
                    .alert { background-color: #fee2e2; border-left: 4px solid #dc2626; padding: 15px; margin: 20px 0; }
                    .details { background-color: #e2e8f0; padding: 15px; border-radius: 4px; margin: 20px 0; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #e2e8f0; font-size: 14px; color: #64748b; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>🔒 TariffSheriff</h1>
                    <p>Security Alert</p>
                </div>
                <div class="content">
                    <h2>Hello %s!</h2>
                    <div class="alert">
                        <p><strong>Security Event Detected:</strong> %s</p>
                    </div>
                    <p>We detected unusual activity on your TariffSheriff account and wanted to notify you immediately.</p>
                    <div class="details">
                        <p><strong>Event Details:</strong></p>
                        <ul>
                            <li><strong>Time:</strong> %s</li>
                            <li><strong>IP Address:</strong> %s</li>
                            <li><strong>User Agent:</strong> %s</li>
                        </ul>
                    </div>
                    <p>If this was you, no action is needed. If you don't recognize this activity, please:</p>
                    <ol>
                        <li>Change your password immediately</li>
                        <li>Review your account settings</li>
                        <li>Contact our support team if needed</li>
                    </ol>
                    <p>Your account security is our top priority.</p>
                </div>
                <div class="footer">
                    <p>Best regards,<br>The TariffSheriff Security Team</p>
                    <p>This is an automated security alert, please do not reply to this email.</p>
                </div>
            </body>
            </html>
            """.formatted(userName, event, timestamp, ipAddress, userAgent);
    }

    private String buildSecurityAlertEmailText(String userName, String event, String ipAddress, String userAgent) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        return """
            Hello %s!
            
            SECURITY ALERT: We detected unusual activity on your TariffSheriff account.
            
            Event: %s
            Time: %s
            IP Address: %s
            User Agent: %s
            
            If this was you, no action is needed. If you don't recognize this activity, please:
            
            1. Change your password immediately
            2. Review your account settings
            3. Contact our support team if needed
            
            Your account security is our top priority.
            
            Best regards,
            The TariffSheriff Security Team
            
            This is an automated security alert, please do not reply to this email.
            """.formatted(userName, event, timestamp, ipAddress, userAgent);
    }

    private String buildWelcomeEmailTemplate(String userName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Welcome to TariffSheriff</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #059669; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background-color: #f8fafc; padding: 30px; border-radius: 0 0 8px 8px; }
                    .button { display: inline-block; background-color: #059669; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; margin: 20px 0; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #e2e8f0; font-size: 14px; color: #64748b; }
                    .features { background-color: #ecfdf5; padding: 20px; border-radius: 6px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>🎉 Welcome to TariffSheriff!</h1>
                </div>
                <div class="content">
                    <h2>Hello %s!</h2>
                    <p>Congratulations! Your email has been verified and your TariffSheriff account is now active.</p>
                    <div class="features">
                        <h3>What you can do now:</h3>
                        <ul>
                            <li>Calculate tariff rates for international trade</li>
                            <li>Access comprehensive trade agreement data</li>
                            <li>Analyze market trends and opportunities</li>
                            <li>Generate detailed trade reports</li>
                        </ul>
                    </div>
                    <p style="text-align: center;">
                        <a href="%s" class="button">Start Using TariffSheriff</a>
                    </p>
                    <p>If you have any questions or need assistance, our support team is here to help.</p>
                </div>
                <div class="footer">
                    <p>Best regards,<br>The TariffSheriff Team</p>
                    <p>This is an automated message, please do not reply to this email.</p>
                </div>
            </body>
            </html>
            """.formatted(userName, frontendUrl);
    }

    private String buildWelcomeEmailText(String userName) {
        return """
            Hello %s!
            
            Congratulations! Your email has been verified and your TariffSheriff account is now active.
            
            What you can do now:
            - Calculate tariff rates for international trade
            - Access comprehensive trade agreement data
            - Analyze market trends and opportunities
            - Generate detailed trade reports
            
            Start using TariffSheriff: %s
            
            If you have any questions or need assistance, our support team is here to help.
            
            Best regards,
            The TariffSheriff Team
            
            This is an automated message, please do not reply to this email.
            """.formatted(userName, frontendUrl);
    }

    private String buildAccountChangeEmailTemplate(String userName, String changeType) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Account Settings Changed</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #2563eb; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background-color: #f8fafc; padding: 30px; border-radius: 0 0 8px 8px; }
                    .change-info { background-color: #dbeafe; border-left: 4px solid #2563eb; padding: 15px; margin: 20px 0; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #e2e8f0; font-size: 14px; color: #64748b; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>TariffSheriff</h1>
                    <p>Account Settings Changed</p>
                </div>
                <div class="content">
                    <h2>Hello %s!</h2>
                    <p>This is to confirm that your account settings have been updated.</p>
                    <div class="change-info">
                        <p><strong>Change Made:</strong> %s</p>
                        <p><strong>Time:</strong> %s</p>
                    </div>
                    <p>If you didn't make this change, please contact our support team immediately and consider changing your password.</p>
                </div>
                <div class="footer">
                    <p>Best regards,<br>The TariffSheriff Team</p>
                    <p>This is an automated message, please do not reply to this email.</p>
                </div>
            </body>
            </html>
            """.formatted(userName, changeType, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    private String buildAccountChangeEmailText(String userName, String changeType) {
        return """
            Hello %s!
            
            This is to confirm that your account settings have been updated.
            
            Change Made: %s
            Time: %s
            
            If you didn't make this change, please contact our support team immediately and consider changing your password.
            
            Best regards,
            The TariffSheriff Team
            
            This is an automated message, please do not reply to this email.
            """.formatted(userName, changeType, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
}