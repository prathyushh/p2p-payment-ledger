package com.p2ppayment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2ppayment.entity.AuditLog;
import com.p2ppayment.entity.User;
import com.p2ppayment.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Converts details string to valid JSON before saving.
     * audit_log.details is JSONB — plain strings are invalid.
     * Wraps as: {"note": "..."}
     */
    @Transactional
    public void logAction(String action, String entityType, Long entityId, User actor, String details, String ipAddress) {
        String jsonDetails = toJson(details);

        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .actor(actor)
                .details(jsonDetails)
                .ipAddress(ipAddress)
                .build();

        auditLogRepository.save(auditLog);
    }

    @Transactional
    public void logAction(String action, String entityType, Long entityId, User actor, String details) {
        logAction(action, entityType, entityId, actor, details, null);
    }

    @Transactional
    public void logWalletStatusChange(String action, Long walletId, String beforeStatus, String afterStatus, User actor, String ipAddress) {
        try {
            String details = objectMapper.writeValueAsString(
                    Map.of("before_status", beforeStatus, "after_status", afterStatus)
            );
            logAction(action, "WALLET", walletId, actor, details, ipAddress);
        } catch (Exception e) {
            logAction(action, "WALLET", walletId, actor,
                    toJson("Status change: " + beforeStatus + " to " + afterStatus), ipAddress);
        }
    }

    /**
     * If the string is already valid JSON (starts with { or [), use it as-is.
     * Otherwise wrap it as {"note": "..."} so it is valid JSONB.
     */
    private String toJson(String details) {
        if (details == null) return null;
        String trimmed = details.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return trimmed; // already valid JSON
        }
        try {
            // Wrap plain text as a JSON object
            return objectMapper.writeValueAsString(Map.of("note", details));
        } catch (Exception e) {
            return "{\"note\": \"audit details unavailable\"}";
        }
    }
}
