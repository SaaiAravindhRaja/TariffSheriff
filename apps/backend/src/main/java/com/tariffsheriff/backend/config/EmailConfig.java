package com.tariffsheriff.backend.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Email configuration for authentication-related notifications.
 * Configures JavaMailSender and email service properties.
 */
@Configuration
@Getter
public class EmailConfig {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${email.from.address}")
    private String fromAddress;

    @Value("${email.from.name}")
    private String fromName;

    @Value("${email.base-url}")
    private String baseUrl;

    @Value("${email.frontend-url}")
    private String frontendUrl;

    /**
     * Configure JavaMailSender with SMTP properties.
     */
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        mailSender.setHost(host);
        mailSender.setPort(port);
        
        // Set credentials if provided
        if (username != null && !username.trim().isEmpty()) {
            mailSender.setUsername(username);
        }
        if (password != null && !password.trim().isEmpty()) {
            mailSender.setPassword(password);
        }

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.debug", "false");
        
        // Additional security properties
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        return mailSender;
    }

    /**
     * Get the complete "from" address with name.
     */
    public String getFromAddressWithName() {
        if (fromName != null && !fromName.trim().isEmpty()) {
            return String.format("%s <%s>", fromName, fromAddress);
        }
        return fromAddress;
    }

    /**
     * Build verification URL for email verification.
     */
    public String buildVerificationUrl(String token) {
        return String.format("%s/verify?token=%s", frontendUrl, token);
    }

    /**
     * Build password reset URL for password reset emails.
     */
    public String buildPasswordResetUrl(String token) {
        return String.format("%s/reset-password?token=%s", frontendUrl, token);
    }
}