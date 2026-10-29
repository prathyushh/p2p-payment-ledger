package com.p2ppayment.controller;

import com.p2ppayment.dto.AdminFraudActionRequest;
import com.p2ppayment.dto.FraudAlertResponse;
import com.p2ppayment.entity.FraudAlert;
import com.p2ppayment.entity.User;
import com.p2ppayment.repository.UserRepository;
import com.p2ppayment.service.AuditService;
import com.p2ppayment.service.FraudService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/fraud")
public class AdminFraudController {

    private final FraudService fraudService;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public AdminFraudController(FraudService fraudService,
                              AuditService auditService,
                              UserRepository userRepository) {
        this.fraudService = fraudService;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    /**
     * Block a HIGH_VALUE fraud alert and refund the transaction sender.
     * Admin-only endpoint.
     *
     * @param alertId fraud alert ID
     * @param request action request with notes
     * @param httpRequest HTTP request for logging IP
     * @return updated fraud alert
     */
    @PutMapping("/{alertId}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FraudAlertResponse> blockAlert(
            @PathVariable Long alertId,
            @Valid @RequestBody AdminFraudActionRequest request,
            HttpServletRequest httpRequest) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long adminId = Long.parseLong(authentication.getName());
        User admin = userRepository.findById(adminId).orElse(null);

        // Block the alert and refund sender
        FraudAlert alert = fraudService.blockAlert(alertId, request.notes());

        // Log admin action
        String ipAddress = getClientIpAddress(httpRequest);
        auditService.logAction("FRAUD_ALERT_BLOCKED", "FRAUD_ALERT", alertId, admin,
                "Alert blocked with notes: " + request.notes(), ipAddress);

        FraudAlertResponse response = toFraudAlertResponse(alert);
        return ResponseEntity.ok(response);
    }

    /**
     * Dismiss a HIGH_VALUE fraud alert without action on the transaction.
     * Admin-only endpoint.
     *
     * @param alertId fraud alert ID
     * @param request action request with notes
     * @param httpRequest HTTP request for logging IP
     * @return updated fraud alert
     */
    @PutMapping("/{alertId}/dismiss")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FraudAlertResponse> dismissAlert(
            @PathVariable Long alertId,
            @Valid @RequestBody AdminFraudActionRequest request,
            HttpServletRequest httpRequest) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long adminId = Long.parseLong(authentication.getName());
        User admin = userRepository.findById(adminId).orElse(null);

        // Dismiss the alert (no transaction change)
        FraudAlert alert = fraudService.dismissAlert(alertId, request.notes());

        // Log admin action
        String ipAddress = getClientIpAddress(httpRequest);
        auditService.logAction("FRAUD_ALERT_DISMISSED", "FRAUD_ALERT", alertId, admin,
                "Alert dismissed with notes: " + request.notes(), ipAddress);

        FraudAlertResponse response = toFraudAlertResponse(alert);
        return ResponseEntity.ok(response);
    }

    /**
     * Convert FraudAlert entity to response DTO.
     */
    private FraudAlertResponse toFraudAlertResponse(FraudAlert alert) {
        return new FraudAlertResponse(
                alert.getId(),
                alert.getTransaction().getId(),
                alert.getAlertType().toString(),
                alert.getStatus().toString(),
                alert.getNotes(),
                alert.getCreatedAt().toString()
        );
    }

    /**
     * Get client IP address from HTTP request.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}