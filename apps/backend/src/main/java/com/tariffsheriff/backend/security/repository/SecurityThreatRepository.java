package com.tariffsheriff.backend.security.repository;

import com.tariffsheriff.backend.security.model.SecurityThreat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for security threats
 */
@Repository
public interface SecurityThreatRepository extends JpaRepository<SecurityThreat, Long> {

    /**
     * Find active threats by severity
     */
    List<SecurityThreat> findByStatusAndSeverityInOrderByDetectedAtDesc(
            SecurityThreat.ThreatStatus status, List<SecurityThreat.ThreatSeverity> severities);

    /**
     * Find threats by user ID within time range
     */
    List<SecurityThreat> findByUserIdAndDetectedAtBetween(
            String userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find threats by client IP within time range
     */
    List<SecurityThreat> findByClientIpAndDetectedAtBetween(
            String clientIp, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find threats by type within time range
     */
    Page<SecurityThreat> findByThreatTypeAndDetectedAtBetween(
            SecurityThreat.ThreatType threatType, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Count active threats by severity
     */
    @Query("SELECT t.severity, COUNT(t) FROM SecurityThreat t WHERE t.status = 'ACTIVE' GROUP BY t.severity")
    List<Object[]> countActiveThreatsBySeverity();

    /**
     * Find recent threats for monitoring dashboard
     */
    List<SecurityThreat> findTop50ByOrderByDetectedAtDesc();

    /**
     * Count threats by user in time range for behavioral analysis
     */
    @Query("SELECT COUNT(t) FROM SecurityThreat t WHERE t.userId = :userId AND t.detectedAt BETWEEN :startTime AND :endTime")
    long countThreatsByUserInTimeRange(@Param("userId") String userId, 
                                      @Param("startTime") LocalDateTime startTime, 
                                      @Param("endTime") LocalDateTime endTime);

    /**
     * Find unresolved high-severity threats
     */
    List<SecurityThreat> findByStatusInAndSeverityInOrderByDetectedAtAsc(
            List<SecurityThreat.ThreatStatus> statuses, List<SecurityThreat.ThreatSeverity> severities);

    /**
     * Get threat statistics for reporting
     */
    @Query("SELECT t.threatType, t.severity, COUNT(t) FROM SecurityThreat t " +
           "WHERE t.detectedAt BETWEEN :startTime AND :endTime " +
           "GROUP BY t.threatType, t.severity")
    List<Object[]> getThreatStatistics(@Param("startTime") LocalDateTime startTime, 
                                      @Param("endTime") LocalDateTime endTime);
}