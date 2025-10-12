package com.tariffsheriff.backend.audit.repository;

import com.tariffsheriff.backend.audit.model.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for audit events
 */
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    /**
     * Find audit events by user ID within a time range
     */
    Page<AuditEvent> findByUserIdAndCreatedAtBetween(
            String userId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Find audit events by event type within a time range
     */
    Page<AuditEvent> findByEventTypeAndCreatedAtBetween(
            AuditEvent.AuditEventType eventType, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Find high-risk audit events
     */
    Page<AuditEvent> findByRiskLevelInAndCreatedAtBetween(
            List<AuditEvent.RiskLevel> riskLevels, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Find audit events involving sensitive data access
     */
    Page<AuditEvent> findBySensitiveDataAccessedTrueAndCreatedAtBetween(
            LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Find audit events by client IP for security analysis
     */
    List<AuditEvent> findByClientIpAndCreatedAtBetween(
            String clientIp, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Count events by user within time range for rate limiting analysis
     */
    @Query("SELECT COUNT(e) FROM AuditEvent e WHERE e.userId = :userId AND e.createdAt BETWEEN :startTime AND :endTime")
    long countByUserIdAndTimeRange(@Param("userId") String userId, 
                                  @Param("startTime") LocalDateTime startTime, 
                                  @Param("endTime") LocalDateTime endTime);

    /**
     * Find events ready for cleanup based on retention policy
     */
    List<AuditEvent> findByRetentionUntilBefore(LocalDateTime cutoffDate);

    /**
     * Get AI usage statistics for compliance reporting
     */
    @Query("SELECT e.aiModelUsed, COUNT(e), SUM(e.aiTokensConsumed) " +
           "FROM AuditEvent e WHERE e.eventType = 'AI_INTERACTION' " +
           "AND e.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY e.aiModelUsed")
    List<Object[]> getAiUsageStatistics(@Param("startTime") LocalDateTime startTime, 
                                       @Param("endTime") LocalDateTime endTime);

    /**
     * Get data access patterns for compliance analysis
     */
    @Query("SELECT e.dataSourcesAccessed, COUNT(e) " +
           "FROM AuditEvent e WHERE e.eventType = 'DATA_ACCESS' " +
           "AND e.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY e.dataSourcesAccessed")
    List<Object[]> getDataAccessPatterns(@Param("startTime") LocalDateTime startTime, 
                                        @Param("endTime") LocalDateTime endTime);
}