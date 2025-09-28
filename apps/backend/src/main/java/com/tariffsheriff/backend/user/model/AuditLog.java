package com.tariffsheriff.backend.user.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for tracking security events and user actions for audit purposes.
 * Provides comprehensive logging of authentication events, user management actions,
 * and security-related activities.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_logs_user_id", columnList = "userId"),
    @Index(name = "idx_audit_logs_action", columnList = "action"),
    @Index(name = "idx_audit_logs_created_at", columnList = "createdAt"),
    @Index(name = "idx_audit_logs_resource_type", columnList = "resourceType"),
    @Index(name = "idx_audit_logs_ip_address", columnList = "ipAddress"),
    @Index(name = "idx_audit_logs_user_action", columnList = "userId, action"),
    @Index(name = "idx_audit_logs_user_created_at", columnList = "userId, createdAt"),
    @Index(name = "idx_audit_logs_action_created_at", columnList = "action, createdAt")
})
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID of the user who performed the action (nullable for system events)
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * Type of action performed
     */
    @NotBlank(message = "Action is required")
    @Size(max = 100, message = "Action must not exceed 100 characters")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    /**
     * Type of resource affected (e.g., USER, TOKEN, SYSTEM)
     */
    @Size(max = 50, message = "Resource type must not exceed 50 characters")
    @Column(name = "resource_type")
    private String resourceType;

    /**
     * ID of the specific resource affected
     */
    @Size(max = 100, message = "Resource ID must not exceed 100 characters")
    @Column(name = "resource_id")
    private String resourceId;

    /**
     * IP address from which the action was performed
     */
    @Size(max = 45, message = "IP address must not exceed 45 characters")
    @Column(name = "ip_address")
    private String ipAddress;

    /**
     * User agent string from the client
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /**
     * Additional details about the action in JSON or text format
     */
    @Column(columnDefinition = "TEXT")
    private String details;

    /**
     * Timestamp when the action occurred
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Optional relationship to the User entity
     * Uses @JoinColumn instead of @ManyToOne to avoid loading user data by default
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    // Constructors
    public AuditLog() {
    }

    public AuditLog(Long userId, AuditAction action, String ipAddress) {
        this.userId = userId;
        this.action = action;
        this.ipAddress = ipAddress;
    }

    public AuditLog(Long userId, AuditAction action, String resourceType, String resourceId, String ipAddress) {
        this.userId = userId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.ipAddress = ipAddress;
    }

    public AuditLog(Long userId, AuditAction action, String resourceType, String resourceId, 
                   String ipAddress, String userAgent, String details) {
        this.userId = userId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.details = details;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // Utility methods
    public boolean isUserAction() {
        return userId != null;
    }

    public boolean isSystemAction() {
        return userId == null;
    }

    public boolean isSecurityEvent() {
        return action.isSecurityEvent();
    }

    public boolean isAuthenticationEvent() {
        return action.isAuthenticationEvent();
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", userId=" + userId +
                ", action=" + action +
                ", resourceType='" + resourceType + '\'' +
                ", resourceId='" + resourceId + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}