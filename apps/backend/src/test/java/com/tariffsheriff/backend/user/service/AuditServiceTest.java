package com.tariffsheriff.backend.user.service;

import com.tariffsheriff.backend.user.model.AuditLog;
import com.tariffsheriff.backend.user.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogRepository);
    }

    @Test
    void shouldLogAuthenticationEvent() {
        // Given
        Long userId = 1L;
        String action = "LOGIN_SUCCESS";
        String details = "User logged in successfully";
        String ipAddress = "192.168.1.1";
        String userAgent = "Mozilla/5.0";

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logAuthenticationEvent(userId, action, details, ipAddress, userAgent);

        // Then
        verify(auditLogRepository).save(auditLogCaptor.capture());
        
        AuditLog capturedLog = auditLogCaptor.getValue();
        assertThat(capturedLog.getUserId()).isEqualTo(userId);
        assertThat(capturedLog.getAction()).isEqualTo(action);
        assertThat(capturedLog.getDetails()).isEqualTo(details);
        assertThat(capturedLog.getIpAddress()).isEqualTo(ipAddress);
        assertThat(capturedLog.getUserAgent()).isEqualTo(userAgent);
        assertThat(capturedLog.getResourceType()).isEqualTo("AUTHENTICATION");
        assertThat(capturedLog.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldLogUserManagementEvent() {
        // Given
        Long adminUserId = 1L;
        Long targetUserId = 2L;
        String action = "USER_ROLE_CHANGED";
        String details = "User role changed from USER to ADMIN";
        String ipAddress = "192.168.1.1";

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logUserManagementEvent(adminUserId, targetUserId, action, details, ipAddress);

        // Then
        verify(auditLogRepository).save(auditLogCaptor.capture());
        
        AuditLog capturedLog = auditLogCaptor.getValue();
        assertThat(capturedLog.getUserId()).isEqualTo(adminUserId);
        assertThat(capturedLog.getAction()).isEqualTo(action);
        assertThat(capturedLog.getDetails()).isEqualTo(details);
        assertThat(capturedLog.getIpAddress()).isEqualTo(ipAddress);
        assertThat(capturedLog.getResourceType()).isEqualTo("USER_MANAGEMENT");
        assertThat(capturedLog.getResourceId()).isEqualTo(targetUserId.toString());
    }

    @Test
    void shouldLogSecurityEvent() {
        // Given
        Long userId = 1L;
        String action = "SUSPICIOUS_ACTIVITY_DETECTED";
        String details = "Multiple failed login attempts from different IPs";
        String ipAddress = "192.168.1.1";

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logSecurityEvent(userId, action, details, ipAddress);

        // Then
        verify(auditLogRepository).save(auditLogCaptor.capture());
        
        AuditLog capturedLog = auditLogCaptor.getValue();
        assertThat(capturedLog.getUserId()).isEqualTo(userId);
        assertThat(capturedLog.getAction()).isEqualTo(action);
        assertThat(capturedLog.getDetails()).isEqualTo(details);
        assertThat(capturedLog.getIpAddress()).isEqualTo(ipAddress);
        assertThat(capturedLog.getResourceType()).isEqualTo("SECURITY");
    }

    @Test
    void shouldLogSystemEvent() {
        // Given
        String action = "SYSTEM_STARTUP";
        String details = "Authentication system started successfully";

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logSystemEvent(action, details);

        // Then
        verify(auditLogRepository).save(auditLogCaptor.capture());
        
        AuditLog capturedLog = auditLogCaptor.getValue();
        assertThat(capturedLog.getUserId()).isNull();
        assertThat(capturedLog.getAction()).isEqualTo(action);
        assertThat(capturedLog.getDetails()).isEqualTo(details);
        assertThat(capturedLog.getResourceType()).isEqualTo("SYSTEM");
    }

    @Test
    void shouldGetAuditLogsForUser() {
        // Given
        Long userId = 1L;
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        
        List<AuditLog> expectedLogs = Arrays.asList(
            createAuditLog(userId, "LOGIN_SUCCESS"),
            createAuditLog(userId, "PASSWORD_CHANGED")
        );
        
        when(auditLogRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, from, to))
            .thenReturn(expectedLogs);

        // When
        List<AuditLog> actualLogs = auditService.getAuditLogsForUser(userId, from, to);

        // Then
        assertThat(actualLogs).isEqualTo(expectedLogs);
        verify(auditLogRepository).findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, from, to);
    }

    @Test
    void shouldGetAuditLogsByAction() {
        // Given
        String action = "LOGIN_FAILED";
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        
        List<AuditLog> expectedLogs = Arrays.asList(
            createAuditLog(1L, action),
            createAuditLog(2L, action)
        );
        
        when(auditLogRepository.findByActionAndCreatedAtBetweenOrderByCreatedAtDesc(action, from, to))
            .thenReturn(expectedLogs);

        // When
        List<AuditLog> actualLogs = auditService.getAuditLogsByAction(action, from, to);

        // Then
        assertThat(actualLogs).isEqualTo(expectedLogs);
        verify(auditLogRepository).findByActionAndCreatedAtBetweenOrderByCreatedAtDesc(action, from, to);
    }

    @Test
    void shouldGetAuditLogsByIpAddress() {
        // Given
        String ipAddress = "192.168.1.1";
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        
        List<AuditLog> expectedLogs = Arrays.asList(
            createAuditLog(1L, "LOGIN_SUCCESS"),
            createAuditLog(1L, "LOGOUT_SUCCESS")
        );
        
        when(auditLogRepository.findByIpAddressAndCreatedAtBetweenOrderByCreatedAtDesc(ipAddress, from, to))
            .thenReturn(expectedLogs);

        // When
        List<AuditLog> actualLogs = auditService.getAuditLogsByIpAddress(ipAddress, from, to);

        // Then
        assertThat(actualLogs).isEqualTo(expectedLogs);
        verify(auditLogRepository).findByIpAddressAndCreatedAtBetweenOrderByCreatedAtDesc(ipAddress, from, to);
    }

    @Test
    void shouldGetSecurityEvents() {
        // Given
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        
        List<AuditLog> expectedLogs = Arrays.asList(
            createSecurityAuditLog(1L, "ACCOUNT_LOCKED"),
            createSecurityAuditLog(2L, "SUSPICIOUS_ACTIVITY_DETECTED")
        );
        
        when(auditLogRepository.findByResourceTypeAndCreatedAtBetweenOrderByCreatedAtDesc("SECURITY", from, to))
            .thenReturn(expectedLogs);

        // When
        List<AuditLog> actualLogs = auditService.getSecurityEvents(from, to);

        // Then
        assertThat(actualLogs).isEqualTo(expectedLogs);
        verify(auditLogRepository).findByResourceTypeAndCreatedAtBetweenOrderByCreatedAtDesc("SECURITY", from, to);
    }

    @Test
    void shouldGetFailedLoginAttempts() {
        // Given
        LocalDateTime from = LocalDateTime.now().minusHours(1);
        LocalDateTime to = LocalDateTime.now();
        
        List<AuditLog> expectedLogs = Arrays.asList(
            createAuditLog(1L, "LOGIN_FAILED"),
            createAuditLog(2L, "LOGIN_FAILED")
        );
        
        when(auditLogRepository.findByActionAndCreatedAtBetweenOrderByCreatedAtDesc("LOGIN_FAILED", from, to))
            .thenReturn(expectedLogs);

        // When
        List<AuditLog> actualLogs = auditService.getFailedLoginAttempts(from, to);

        // Then
        assertThat(actualLogs).isEqualTo(expectedLogs);
        verify(auditLogRepository).findByActionAndCreatedAtBetweenOrderByCreatedAtDesc("LOGIN_FAILED", from, to);
    }

    @Test
    void shouldCountEventsByAction() {
        // Given
        String action = "LOGIN_SUCCESS";
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        long expectedCount = 25L;
        
        when(auditLogRepository.countByActionAndCreatedAtBetween(action, from, to))
            .thenReturn(expectedCount);

        // When
        long actualCount = auditService.countEventsByAction(action, from, to);

        // Then
        assertThat(actualCount).isEqualTo(expectedCount);
        verify(auditLogRepository).countByActionAndCreatedAtBetween(action, from, to);
    }

    @Test
    void shouldCountEventsByUser() {
        // Given
        Long userId = 1L;
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        long expectedCount = 10L;
        
        when(auditLogRepository.countByUserIdAndCreatedAtBetween(userId, from, to))
            .thenReturn(expectedCount);

        // When
        long actualCount = auditService.countEventsByUser(userId, from, to);

        // Then
        assertThat(actualCount).isEqualTo(expectedCount);
        verify(auditLogRepository).countByUserIdAndCreatedAtBetween(userId, from, to);
    }

    @Test
    void shouldHandleNullValuesGracefully() {
        // Given
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logAuthenticationEvent(null, "LOGIN_FAILED", "User not found", null, null);

        // Then
        verify(auditLogRepository).save(auditLogCaptor.capture());
        
        AuditLog capturedLog = auditLogCaptor.getValue();
        assertThat(capturedLog.getUserId()).isNull();
        assertThat(capturedLog.getIpAddress()).isNull();
        assertThat(capturedLog.getUserAgent()).isNull();
        assertThat(capturedLog.getAction()).isEqualTo("LOGIN_FAILED");
        assertThat(capturedLog.getDetails()).isEqualTo("User not found");
    }

    @Test
    void shouldTruncateLongDetails() {
        // Given
        String longDetails = "A".repeat(2000); // Very long string
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logAuthenticationEvent(1L, "TEST_ACTION", longDetails, "192.168.1.1", null);

        // Then
        verify(auditLogRepository).save(auditLogCaptor.capture());
        
        AuditLog capturedLog = auditLogCaptor.getValue();
        assertThat(capturedLog.getDetails()).hasSizeLessThanOrEqualTo(1000); // Assuming max length is 1000
    }

    private AuditLog createAuditLog(Long userId, String action) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setResourceType("AUTHENTICATION");
        log.setDetails("Test details");
        log.setIpAddress("192.168.1.1");
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }

    private AuditLog createSecurityAuditLog(Long userId, String action) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setResourceType("SECURITY");
        log.setDetails("Security event details");
        log.setIpAddress("192.168.1.1");
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }
}