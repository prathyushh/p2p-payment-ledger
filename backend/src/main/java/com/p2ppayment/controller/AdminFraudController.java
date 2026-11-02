package com.p2ppayment.controller;

import com.p2ppayment.dto.AdminFraudActionRequest;
import com.p2ppayment.dto.FraudAlertResponse;
import com.p2ppayment.entity.FraudAlert;
import com.p2ppayment.entity.User;
import com.p2ppayment.repository.FraudAlertRepository;
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

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/fraud")
public class AdminFraudController {

    private final FraudService fraudService;
    private final FraudAlertRepository fraudAlertRepository;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public AdminFraudController(FraudService fraudService,
                                FraudAlertRepository fraudAlertRepository,
                                AuditService auditService,
                                UserRepository userRepository) {
        this.fraudService = fraudService;
        this.fraudAlertRepository = fraudAlertRepository;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    /**
     * Get all fraud alerts ordered by most recent first.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FraudAlertResponse>> getAllAlerts() {
        List<FraudAlert> alerts = fraudAlertRepository.findAllByOrderByCreatedAtDesc();
        List<FraudAlertResponse> response = alerts.stream()
                .map(this::toFraudAlertResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Block a HIGH_VALUE fraud alert and refund the transaction sender.
     */
    @PutMapping("/{alertId}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FraudAlertResponse> blockAlert(
            @PathVariable Long alertId,
            @Valid @RequestBody AdminFraudActionRequest request,
            HttpServletRequest httpRequest) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long adminId = Long.parseLong(auth.getName());
        User admin = userRepository.findById(adminId).orElse(null);

        FraudAlert alert = fraudService.blockAlert(alertId, request.notes());

        auditService.logAction("FRAUD_ALERT_BLOCKED", "FRAUD_ALERT", alertId, admin,
                "Alert blocked with notes: " + request.notes(), getClientIp(httpRequest));

        return ResponseEntity.ok(toFraudAlertResponse(alert));
    }

    /**
     * Dismiss a HIGH_VALUE fraud alert without acting on the transaction.
     */
    @PutMapping("/{alertId}/dismiss")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FraudAlertResponse> dismissAlert(
            @PathVariable Long alertId,
            @Valid @RequestBody AdminFraudActionRequest request,
            HttpServletRequest httpRequest) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long adminId = Long.parseLong(auth.getName());
        User admin = userRepository.findById(adminId).orElse(null);

        FraudAlert alert = fraudService.dismissAlert(alertId, request.notes());

        auditService.logAction("FRAUD_ALERT_DISMISSED", "FRAUD_ALERT", alertId, admin,
                "Alert dismissed with notes: " + request.notes(), getClientIp(httpRequest));

        return ResponseEntity.ok(toFraudAlertResponse(alert));
    }

    private FraudAlertResponse toFraudAlertResponse(FraudAlert alert) {
        String transactionStatus = alert.getTransaction() != null
                ? alert.getTransaction().getStatus().toString() : null;
        return new FraudAlertResponse(
                alert.getId(),
                alert.getTransaction() != null ? alert.getTransaction().getId() : null,
                alert.getAlertType().toString(),
                alert.getStatus().toString(),
                alert.getNotes(),
                alert.getCreatedAt().toString(),
                transactionStatus
        );
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
