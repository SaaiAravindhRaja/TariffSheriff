package com.tariffsheriff.backend.user.service;

import com.tariffsheriff.backend.user.model.User;
import com.tariffsheriff.backend.user.model.UserRole;
import com.tariffsheriff.backend.user.model.UserStatus;
import com.tariffsheriff.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceAuthTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private EmailService emailService;

    private UserService userService;
    private User testUser;
    private final String testEmail = "test@example.com";
    private final String testPassword = "TestPassword123!";

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, emailService);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail(testEmail);
        testUser.setName("Test User");
        testUser.setRole(UserRole.USER);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setEmailVerified(true);
        testUser.setFailedLoginAttempts(0);
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void shouldCheckIfEmailIsRegistered() {
        // Given
        when(userRepository.existsByEmail(testEmail)).thenReturn(true);

        // When
        boolean isRegistered = userService.isEmailRegistered(testEmail);

        // Then
        assertThat(isRegistered).isTrue();
        verify(userRepository).existsByEmail(testEmail);
    }

    @Test
    void shouldValidateStrongPassword() {
        // Given
        String strongPassword = "StrongPassword123!";

        // When
        boolean isValid = userService.isPasswordValid(strongPassword);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldRejectWeakPasswords() {
        // Test cases for weak passwords
        assertThat(userService.isPasswordValid("weak")).isFalse(); // Too short
        assertThat(userService.isPasswordValid("password")).isFalse(); // No uppercase, numbers, special chars
        assertThat(userService.isPasswordValid("PASSWORD")).isFalse(); // No lowercase, numbers, special chars
        assertThat(userService.isPasswordValid("Password")).isFalse(); // No numbers, special chars
        assertThat(userService.isPasswordValid("Password123")).isFalse(); // No special chars
        assertThat(userService.isPasswordValid("password123!")).isFalse(); // No uppercase
    }

    @Test
    void shouldCreateUserWithVerification() {
        // Given
        String encodedPassword = "encoded-password";
        when(passwordEncoder.encode(testPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        // When
        User createdUser = userService.createUserWithVerification(
            "Test User", testEmail, testPassword, UserRole.USER
        );

        // Then
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getEmail()).isEqualTo(testEmail);
        assertThat(createdUser.getName()).isEqualTo("Test User");
        assertThat(createdUser.getRole()).isEqualTo(UserRole.USER);
        assertThat(createdUser.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(createdUser.isEmailVerified()).isFalse();
        assertThat(createdUser.getVerificationToken()).isNotNull();
        assertThat(createdUser.getVerificationTokenExpires()).isAfter(LocalDateTime.now());

        verify(passwordEncoder).encode(testPassword);
        verify(userRepository).save(any(User.class));
        verify(emailService).sendVerificationEmail(eq(createdUser), anyString());
    }

    @Test
    void shouldActivateUserWithValidToken() {
        // Given
        String verificationToken = UUID.randomUUID().toString();
        testUser.setStatus(UserStatus.PENDING);
        testUser.setEmailVerified(false);
        testUser.setVerificationToken(verificationToken);
        testUser.setVerificationTokenExpires(LocalDateTime.now().plusHours(1));

        when(userRepository.findByVerificationToken(verificationToken)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        boolean activated = userService.activateUser(verificationToken);

        // Then
        assertThat(activated).isTrue();
        assertThat(testUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(testUser.isEmailVerified()).isTrue();
        assertThat(testUser.getVerificationToken()).isNull();
        assertThat(testUser.getVerificationTokenExpires()).isNull();

        verify(userRepository).save(testUser);
    }

    @Test
    void shouldNotActivateUserWithExpiredToken() {
        // Given
        String verificationToken = UUID.randomUUID().toString();
        testUser.setStatus(UserStatus.PENDING);
        testUser.setEmailVerified(false);
        testUser.setVerificationToken(verificationToken);
        testUser.setVerificationTokenExpires(LocalDateTime.now().minusHours(1)); // Expired

        when(userRepository.findByVerificationToken(verificationToken)).thenReturn(Optional.of(testUser));

        // When
        boolean activated = userService.activateUser(verificationToken);

        // Then
        assertThat(activated).isFalse();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldNotActivateUserWithInvalidToken() {
        // Given
        String verificationToken = "invalid-token";
        when(userRepository.findByVerificationToken(verificationToken)).thenReturn(Optional.empty());

        // When
        boolean activated = userService.activateUser(verificationToken);

        // Then
        assertThat(activated).isFalse();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldCheckIfUserCanLogin() {
        // Test active user with verified email
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setEmailVerified(true);
        testUser.setAccountLockedUntil(null);
        assertThat(userService.canUserLogin(testUser)).isTrue();

        // Test suspended user
        testUser.setStatus(UserStatus.SUSPENDED);
        assertThat(userService.canUserLogin(testUser)).isFalse();

        // Test locked user
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setAccountLockedUntil(LocalDateTime.now().plusHours(1));
        assertThat(userService.canUserLogin(testUser)).isFalse();

        // Test user with expired lock
        testUser.setAccountLockedUntil(LocalDateTime.now().minusHours(1));
        assertThat(userService.canUserLogin(testUser)).isTrue();
    }

    @Test
    void shouldRecordFailedLoginAttempt() {
        // Given
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.recordFailedLoginAttempt(testEmail);

        // Then
        assertThat(testUser.getFailedLoginAttempts()).isEqualTo(1);
        verify(userRepository).save(testUser);
    }

    @Test
    void shouldLockAccountAfterMaxFailedAttempts() {
        // Given
        testUser.setFailedLoginAttempts(4); // One less than max (5)
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.recordFailedLoginAttempt(testEmail);

        // Then
        assertThat(testUser.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(testUser.getAccountLockedUntil()).isAfter(LocalDateTime.now());
        verify(userRepository).save(testUser);
    }

    @Test
    void shouldUpdateLastLogin() {
        // Given
        String ipAddress = "192.168.1.1";
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.updateLastLogin(testUser.getId(), ipAddress);

        // Then
        assertThat(testUser.getLastLogin()).isNotNull();
        assertThat(testUser.getLastLoginIp()).isEqualTo(ipAddress);
        assertThat(testUser.getFailedLoginAttempts()).isZero(); // Should reset failed attempts
        verify(userRepository).save(testUser);
    }

    @Test
    void shouldInitiatePasswordReset() {
        // Given
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.initiatePasswordReset(testEmail);

        // Then
        assertThat(testUser.getPasswordResetToken()).isNotNull();
        assertThat(testUser.getPasswordResetTokenExpires()).isAfter(LocalDateTime.now());
        verify(userRepository).save(testUser);
        verify(emailService).sendPasswordResetEmail(eq(testUser), anyString());
    }

    @Test
    void shouldNotInitiatePasswordResetForNonExistentUser() {
        // Given
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());

        // When
        userService.initiatePasswordReset(testEmail);

        // Then
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendPasswordResetEmail(any(User.class), anyString());
    }

    @Test
    void shouldResetPasswordWithValidToken() {
        // Given
        String resetToken = UUID.randomUUID().toString();
        String newPassword = "NewPassword123!";
        String encodedPassword = "encoded-new-password";
        
        testUser.setPasswordResetToken(resetToken);
        testUser.setPasswordResetTokenExpires(LocalDateTime.now().plusHours(1));

        when(userRepository.findByPasswordResetToken(resetToken)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        boolean reset = userService.resetPassword(resetToken, newPassword);

        // Then
        assertThat(reset).isTrue();
        assertThat(testUser.getPassword()).isEqualTo(encodedPassword);
        assertThat(testUser.getPasswordResetToken()).isNull();
        assertThat(testUser.getPasswordResetTokenExpires()).isNull();
        verify(userRepository).save(testUser);
    }

    @Test
    void shouldNotResetPasswordWithExpiredToken() {
        // Given
        String resetToken = UUID.randomUUID().toString();
        String newPassword = "NewPassword123!";
        
        testUser.setPasswordResetToken(resetToken);
        testUser.setPasswordResetTokenExpires(LocalDateTime.now().minusHours(1)); // Expired

        when(userRepository.findByPasswordResetToken(resetToken)).thenReturn(Optional.of(testUser));

        // When
        boolean reset = userService.resetPassword(resetToken, newPassword);

        // Then
        assertThat(reset).isFalse();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldChangePasswordWithCorrectCurrentPassword() {
        // Given
        String currentPassword = "CurrentPassword123!";
        String newPassword = "NewPassword123!";
        String encodedNewPassword = "encoded-new-password";
        
        testUser.setPassword("encoded-current-password");

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        boolean changed = userService.changePassword(testUser.getId(), currentPassword, newPassword);

        // Then
        assertThat(changed).isTrue();
        assertThat(testUser.getPassword()).isEqualTo(encodedNewPassword);
        verify(userRepository).save(testUser);
    }

    @Test
    void shouldNotChangePasswordWithIncorrectCurrentPassword() {
        // Given
        String currentPassword = "WrongPassword";
        String newPassword = "NewPassword123!";
        
        testUser.setPassword("encoded-current-password");

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(false);

        // When
        boolean changed = userService.changePassword(testUser.getId(), currentPassword, newPassword);

        // Then
        assertThat(changed).isFalse();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldLockUser() {
        // Given
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.lockUser(testUser.getId());

        // Then
        assertThat(testUser.getStatus()).isEqualTo(UserStatus.LOCKED);
        verify(userRepository).save(testUser);
    }

    @Test
    void shouldUnlockUser() {
        // Given
        testUser.setStatus(UserStatus.LOCKED);
        testUser.setAccountLockedUntil(LocalDateTime.now().plusHours(1));
        testUser.setFailedLoginAttempts(5);

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.unlockUser(testUser.getId());

        // Then
        assertThat(testUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(testUser.getAccountLockedUntil()).isNull();
        assertThat(testUser.getFailedLoginAttempts()).isZero();
        verify(userRepository).save(testUser);
    }

    @Test
    void shouldHandleUserNotFoundGracefully() {
        // Given
        Long nonExistentUserId = 999L;
        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.lockUser(nonExistentUserId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("User not found");

        assertThatThrownBy(() -> userService.unlockUser(nonExistentUserId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("User not found");

        assertThatThrownBy(() -> userService.updateLastLogin(nonExistentUserId, "192.168.1.1"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("User not found");
    }
}