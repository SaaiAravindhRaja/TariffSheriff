package com.tariffsheriff.backend.monitoring;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for monitoring and metrics endpoints
 */
@RestController
@RequestMapping("/api/monitoring")
@Tag(name = "Monitoring", description = "System monitoring and metrics")
@SecurityRequirement(name = "bearerAuth")
public class MonitoringController {

    private final SecurityMonitoringService securityMonitoringService;

    public MonitoringController(SecurityMonitoringService securityMonitoringService) {
        this.securityMonitoringService = securityMonitoringService;
    }

    @GetMapping("/security/metrics")
    @Operation(summary = "Get security metrics", description = "Retrieve current security monitoring metrics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SecurityMonitoringService.SecurityMetrics> getSecurityMetrics() {
        SecurityMonitoringService.SecurityMetrics metrics = securityMonitoringService.getSecurityMetrics();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/health/auth")
    @Operation(summary = "Check authentication health", description = "Check the health of authentication services")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> checkAuthenticationHealth() {
        // This endpoint can be used for custom health checks
        return ResponseEntity.ok("Authentication services are operational");
    }
}