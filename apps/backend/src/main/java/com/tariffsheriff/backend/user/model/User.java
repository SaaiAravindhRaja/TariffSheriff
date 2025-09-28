package com.tariffsheriff.backend.user.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_status", columnList = "status"),
    @Index(name = "idx_users_email_verified", columnList = "emailVerified"),
    @Index(name = "idx_users_verification_token", columnList = "verificationToken"),
    @Index(name = "idx_users_password_reset_token", columnList = "passwordResetToken"),
    @Index(name = "idx_users_last_login", columnList = "lastLogin"),
    @Index(name = "idx_users_created_at", columnList = "createdAt")
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Column(nullable = false, unique = true)
    private String email;

    @Size(max = 500, message = "About me must not exceed 500 characters")
    @Column(length = 500)
    private String aboutMe;

    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @NotBlank(message = "Password is required")
    @Size(max = 255, message = "Password hash must not exceed 255 characters")
    @Column(nullable = false)
    private String password;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Size(max = 255, message = "Verification token must not exceed 255 characters")
    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "verification_token_expires")
    private LocalDateTime verificationTokenExpires;

    @Size(max = 255, message = "Password reset token must not exceed 255 characters")
    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "password_reset_token_expires")
    private LocalDateTime passwordResetTokenExpires;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Size(max = 45, message = "IP address must not exceed 45 characters")
    @Column(name = "last_login_ip")
    private String lastLoginIp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Legacy admin field for backward compatibility
    @Column(nullable = false)
    private boolean admin;

    // Constructors
    public User() {
        this.status = UserStatus.PENDING;
        this.role = UserRole.USER;
        this.emailVerified = false;
        this.failedLoginAttempts = 0;
        this.admin = false;
    }

    public User(String name, String email, String password, UserRole role) {
        this();
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.admin = role == UserRole.ADMIN;
    }

    // Legacy constructor for backward compatibility
    public User(Long id, String name, String email, String aboutMe, String role, String password, boolean admin) {
        this();
        this.id = id;
        this.name = name;
        this.email = email;
        this.aboutMe = aboutMe;
        this.role = UserRole.valueOf(role);
        this.password = password;
        this.admin = admin;
        this.status = UserStatus.ACTIVE; // Assume existing users are active
        this.emailVerified = true; // Assume existing users are verified
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getAboutMe() {
        return aboutMe;
    }
    public void setAboutMe(String aboutMe) {
        this.aboutMe = aboutMe;
    }

    public UserRole getRole() {
        return role;
    }
    public void setRole(UserRole role) {
        this.role = role;
        this.admin = (role == UserRole.ADMIN); // Keep admin field in sync
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public UserStatus getStatus() {
        return status;
    }
    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }
    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getVerificationToken() {
        return verificationToken;
    }
    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public LocalDateTime getVerificationTokenExpires() {
        return verificationTokenExpires;
    }
    public void setVerificationTokenExpires(LocalDateTime verificationTokenExpires) {
        this.verificationTokenExpires = verificationTokenExpires;
    }

    public String getPasswordResetToken() {
        return passwordResetToken;
    }
    public void setPasswordResetToken(String passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }

    public LocalDateTime getPasswordResetTokenExpires() {
        return passwordResetTokenExpires;
    }
    public void setPasswordResetTokenExpires(LocalDateTime passwordResetTokenExpires) {
        this.passwordResetTokenExpires = passwordResetTokenExpires;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }
    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getAccountLockedUntil() {
        return accountLockedUntil;
    }
    public void setAccountLockedUntil(LocalDateTime accountLockedUntil) {
        this.accountLockedUntil = accountLockedUntil;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }
    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Legacy admin field for backward compatibility
    public boolean isAdmin() {
        return admin || (role == UserRole.ADMIN);
    }
    public void setAdmin(boolean admin) {
        this.admin = admin;
        if (admin && role != UserRole.ADMIN) {
            this.role = UserRole.ADMIN;
        }
    }

    // Utility methods
    public boolean canLogin() {
        return status.canLogin() && !isAccountLocked();
    }

    public boolean isAccountLocked() {
        return accountLockedUntil != null && accountLockedUntil.isAfter(LocalDateTime.now());
    }

    public boolean needsEmailVerification() {
        return !emailVerified && status == UserStatus.PENDING;
    }

    public boolean isVerificationTokenValid() {
        return verificationToken != null && 
               verificationTokenExpires != null && 
               verificationTokenExpires.isAfter(LocalDateTime.now());
    }

    public boolean isPasswordResetTokenValid() {
        return passwordResetToken != null && 
               passwordResetTokenExpires != null && 
               passwordResetTokenExpires.isAfter(LocalDateTime.now());
    }

    public void clearVerificationToken() {
        this.verificationToken = null;
        this.verificationTokenExpires = null;
    }

    public void clearPasswordResetToken() {
        this.passwordResetToken = null;
        this.passwordResetTokenExpires = null;
    }

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
    }

    public void lockAccount(LocalDateTime until) {
        this.accountLockedUntil = until;
        this.status = UserStatus.LOCKED;
    }

    public void unlockAccount() {
        this.accountLockedUntil = null;
        this.failedLoginAttempts = 0;
        if (this.status == UserStatus.LOCKED) {
            this.status = UserStatus.ACTIVE;
        }
    }
}
