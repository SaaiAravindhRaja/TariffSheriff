package com.tariffsheriff.backend.user.service;

import com.tariffsheriff.backend.user.model.User;
import com.tariffsheriff.backend.user.model.UserRole;
import com.tariffsheriff.backend.user.model.UserStatus;
import com.tariffsheriff.backend.user.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Account lockout configuration
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;
    private static final int VERIFICATION_TOKEN_EXPIRY_HOURS = 24;
    private static final int PASSWORD_RESET_TOKEN_EXPIRY_HOURS = 1;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        } else {
            return false;
        }
    }

    public Optional<User> updateUser(Long id, User updatedUser) {
        return userRepository.findById(id).map(existingUser -> {
            existingUser.setName(updatedUser.getName());
            existingUser.setEmail(updatedUser.getEmail());
            existingUser.setAboutMe(updatedUser.getAboutMe());
            existingUser.setRole(updatedUser.getRole());
            existingUser.setAdmin(updatedUser.isAdmin());
            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }

            return userRepository.save(existingUser);
        });
    }

    // ========== Authentication-specific methods ==========

    /**
     * Create a new user with email verification token
     * Requirements: 1.1, 1.3
     */
    public User createUserWithVerification(String name, String email, String password, UserRole role) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("User with email " + email + " already exists");
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role != null ? role : UserRole.USER);
        user.setStatus(UserStatus.PENDING);
        user.setEmailVerified(false);
        
        // Generate verification token
        String verificationToken = generateSecureToken();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpires(LocalDateTime.now().plusHours(VERIFICATION_TOKEN_EXPIRY_HOURS));

        return userRepository.save(user);
    }

    /**
     * Activate user account using verification token
     * Requirements: 1.1, 1.3
     */
    public boolean activateUser(String verificationToken) {
        Optional<User> userOpt = findByVerificationToken(verificationToken);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        if (!user.isVerificationTokenValid()) {
            return false;
        }

        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        user.clearVerificationToken();
        userRepository.save(user);
        
        return true;
    }

    /**
     * Generate and set password reset token
     * Requirements: 4.1, 4.2
     */
    public boolean initiatePasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false; // Don't reveal if email exists
        }

        User user = userOpt.get();
        String resetToken = generateSecureToken();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpires(LocalDateTime.now().plusHours(PASSWORD_RESET_TOKEN_EXPIRY_HOURS));
        
        userRepository.save(user);
        return true;
    }

    /**
     * Reset password using reset token
     * Requirements: 4.1, 4.2, 4.3
     */
    public boolean resetPassword(String resetToken, String newPassword) {
        Optional<User> userOpt = findByPasswordResetToken(resetToken);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        if (!user.isPasswordResetTokenValid()) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.clearPasswordResetToken();
        user.resetFailedLoginAttempts(); // Reset any lockout
        
        userRepository.save(user);
        return true;
    }

    /**
     * Change password for authenticated user
     * Requirements: 4.3, 4.4
     */
    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

    /**
     * Lock user account
     * Requirements: 2.5, 6.3
     */
    public void lockUser(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setStatus(UserStatus.LOCKED);
            user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
            userRepository.save(user);
        });
    }

    /**
     * Unlock user account
     * Requirements: 2.5, 6.3
     */
    public void unlockUser(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.unlockAccount();
            userRepository.save(user);
        });
    }

    /**
     * Update last login information
     * Requirements: 2.6, 6.3
     */
    public void updateLastLogin(Long userId, String ipAddress) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            user.setLastLoginIp(ipAddress);
            user.resetFailedLoginAttempts(); // Reset on successful login
            userRepository.save(user);
        });
    }

    /**
     * Record failed login attempt and handle account lockout
     * Requirements: 2.5, 6.3
     */
    public void recordFailedLoginAttempt(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.incrementFailedLoginAttempts();
            
            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.lockAccount(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
            }
            
            userRepository.save(user);
        });
    }

    /**
     * Check if user can login (account status and lockout)
     * Requirements: 2.5, 6.3
     */
    public boolean canUserLogin(User user) {
        return user.canLogin();
    }

    /**
     * Find user by email
     * Requirements: 2.1, 2.2
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Find user by verification token
     */
    private Optional<User> findByVerificationToken(String token) {
        return userRepository.findByVerificationToken(token);
    }

    /**
     * Find user by password reset token
     */
    private Optional<User> findByPasswordResetToken(String token) {
        return userRepository.findByPasswordResetToken(token);
    }

    /**
     * Generate a secure random token
     */
    private String generateSecureToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Validate password strength
     * Requirements: 4.4
     */
    public boolean isPasswordValid(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);
        
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    /**
     * Check if email is already registered
     * Requirements: 1.4
     */
    public boolean isEmailRegistered(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}
