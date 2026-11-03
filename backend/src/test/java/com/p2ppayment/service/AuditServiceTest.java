package com.p2ppayment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2ppayment.entity.AuditLog;
import com.p2ppayment.entity.User;
import com.p2ppayment.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuditService auditService;

    private User testUser;
    private AuditLog testAuditLog;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testAuditLog = new AuditLog();
        testAuditLog.setId(1L);
        testAuditLog.setAction("WALLET_FREEZE");
        testAuditLog.setActor(testUser);
    }

    @Test
    void logWalletStatusChange_Success() {
        // Arrange
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenReturn(testAuditLog);

        // Act
        auditService.logWalletStatusChange(
                "WALLET_FREEZE",
                1L,
                "ACTIVE",
                "FROZEN",
                testUser,
                "127.0.0.1"
        );

        // Assert
        verify(auditLogRepository, times(1))
                .save(any(AuditLog.class));
    }

    @Test
    void logAdminAction_Success() {
        // Arrange
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenReturn(testAuditLog);

        // Act
        auditService.logAction(
                "USER_SEARCH",
                "USER",
                1L,
                testUser,
                "Searched for users",
                "127.0.0.1"
        );

        // Assert
        verify(auditLogRepository, times(1))
                .save(any(AuditLog.class));
    }
}