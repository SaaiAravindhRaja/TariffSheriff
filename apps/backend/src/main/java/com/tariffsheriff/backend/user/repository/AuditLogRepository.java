package com.tariffsheriff.backend.user.repository;

import com.tariffsheriff.backend.user.model.AuditAction;
import com.tariffsheriff.backend.user.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for AuditLog entity operations.
 * Provides methods for querying audit logs with various filters and criteria.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find audit logs by user ID with pagination
     * @param userId the user ID to search for
     * @param pageable pagination information
     * @return page of audit logs for the user
     */
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find audit logs by action type with pagination
     * @param action the action type to search for
     * @param pageable pagination information
     * @return page of audit logs for the action
     */
    Page<AuditLog> findByActionOrderByCreatedAtDesc(AuditAction action, Pageable pageable);

    /**
     * Find audit logs by user ID and action type
     * @param userId the user ID to search for
     * @param action the action type to search for
     * @param pageable pagination information
     * @return page of audit logs matching the criteria
     */
    Page<AuditLog> findByUserIdAndActionOrderByCreatedAtDesc(Long userId, AuditAction action, Pageable pageable);

    /**
     * Find audit logs within a date range
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @param pageable pagination information
     * @return page of audit logs within the date range
     */
    Page<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Find audit logs by IP address
     * @param ipAddress the IP address to search for
     * @param pageable pagination information
     * @return page of audit logs from the IP address
     */
    Page<AuditLog> findByIpAddressOrderByCreatedAtDesc(String ipAddress, Pageable pageable);

    /**
     * Find recent failed login attempts for a user
     * @param userId the user ID to search for
     * @param since the time threshold (only logs after this time)
     * @return list of failed login attempts
     */
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.action = 'USER_LOGIN_FAILED' AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentFailedLoginAttempts(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * Find recent failed login attempts from an IP address
     * @param ipAddress the IP address to search for
     * @param since the time threshold (only logs after this time)
     * @return list of failed login attempts from the IP
     */
    @Query("SELECT a FROM AuditLog a WHERE a.ipAddress = :ipAddress AND a.action = 'USER_LOGIN_FAILED' AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentFailedLoginAttemptsByIp(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);

    /**
     * Find security events for a user within a time period
     * @param userId the user ID to search for
     * @param actions list of security-related actions
     * @param since the time threshold
     * @return list of security events
     */
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.action IN :actions AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<AuditLog> findSecurityEventsByUser(@Param("userId") Long userId, @Param("actions") List<AuditAction> actions, @Param("since") LocalDateTime since);

    /**
     * Find all security events within a time period
     * @param actions list of security-related actions
     * @param since the time threshold
     * @param pageable pagination information
     * @return page of security events
     */
    @Query("SELECT a FROM AuditLog a WHERE a.action IN :actions AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    Page<AuditLog> findSecurityEvents(@Param("actions") List<AuditAction> actions, @Param("since") LocalDateTime since, Pageable pageable);

    /**
     * Count audit logs by action type within a time period
     * @param action the action type to count
     * @param since the time threshold
     * @return count of audit logs
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action = :action AND a.createdAt >= :since")
    long countByActionSince(@Param("action") AuditAction action, @Param("since") LocalDateTime since);

    /**
     * Count failed login attempts for a user within a time period
     * @param userId the user ID to search for
     * @param since the time threshold
     * @return count of failed login attempts
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.userId = :userId AND a.action = 'USER_LOGIN_FAILED' AND a.createdAt >= :since")
    long countFailedLoginAttempts(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * Count failed login attempts from an IP address within a time period
     * @param ipAddress the IP address to search for
     * @param since the time threshold
     * @return count of failed login attempts from the IP
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.ipAddress = :ipAddress AND a.action = 'USER_LOGIN_FAILED' AND a.createdAt >= :since")
    long countFailedLoginAttemptsByIp(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);

    /**
     * Find the most recent successful login for a user
     * @param userId the user ID to search for
     * @return the most recent successful login audit log, or null if none found
     */
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.action = 'USER_LOGIN_SUCCESS' ORDER BY a.createdAt DESC LIMIT 1")
    AuditLog findMostRecentSuccessfulLogin(@Param("userId") Long userId);

    /**
     * Find audit logs by resource type and resource ID
     * @param resourceType the resource type to search for
     * @param resourceId the resource ID to search for
     * @param pageable pagination information
     * @return page of audit logs for the resource
     */
    Page<AuditLog> findByResourceTypeAndResourceIdOrderByCreatedAtDesc(String resourceType, String resourceId, Pageable pageable);

    /**
     * Delete old audit logs before a certain date (for cleanup purposes)
     * @param before the date before which logs should be deleted
     * @return number of deleted records
     */
    @Query("DELETE FROM AuditLog a WHERE a.createdAt < :before")
    int deleteOldLogs(@Param("before") LocalDateTime before);
}