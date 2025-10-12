package com.tariffsheriff.backend.security.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Security threat entity for tracking detected threats
 */
@Entity
@Table(name = "security_threats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecurityThreat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "threat_id", nullable = false, unique = true)
    private String threatId;

    @Column(name = "threat_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ThreatType threatType;

    @Column(name = "severity", nullable = false)
    @Enumerated(EnumType.STRING)
    private ThreatSeverity severity;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ThreatStatus status = ThreatStatus.ACTIVE;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "client_ip")
    private String clientIp;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "detection_rule")
    private String detectionRule;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence; // JSON string containing evidence data

    @Column(name = "mitigation_actions", columnDefinition = "TEXT")
    private String mitigationActions; // JSON string containing actions taken

    @Column(name = "false_positive")
    private Boolean falsePositive = false;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PrePersist
    protected void onCreate() {
        if (detectedAt == null) {
            detectedAt = LocalDateTime.now();
        }
        lastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    public enum ThreatType {
        PROMPT_INJECTION,
        BRUTE_FORCE_ATTACK,
        RATE_LIMIT_VIOLATION,
        SUSPICIOUS_BEHAVIOR,
        DATA_EXFILTRATION,
        UNAUTHORIZED_ACCESS,
        MALICIOUS_INPUT,
        ANOMALOUS_USAGE,
        CREDENTIAL_STUFFING,
        BOT_ACTIVITY
    }

    public enum ThreatSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum ThreatStatus {
        ACTIVE,
        INVESTIGATING,
        MITIGATED,
        RESOLVED,
        FALSE_POSITIVE
    }
}