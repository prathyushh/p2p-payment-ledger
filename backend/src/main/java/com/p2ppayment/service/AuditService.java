package com.p2ppayment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2ppayment.entity.AuditLog;
import com.p2ppayment.entity.User;
import com.p2ppayment.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void logAction(String action, String entityType, Long entityId, User actor, String details, String ipAddress) {
        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .actor(actor)
                .details(details)
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
                    new Object() {
                        public final String before_status = beforeStatus;
                        public final String after_status = afterStatus;
                    }
            );
            logAction(action, "WALLET", walletId, actor, details, ipAddress);
        } catch (Exception e) {
            logAction(action, "WALLET", walletId, actor, "Status change: " + beforeStatus + " → " + afterStatus, ipAddress);
        }
    }
}