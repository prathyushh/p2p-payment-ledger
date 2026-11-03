package com.p2ppayment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2ppayment.entity.AuditLog;
import com.p2ppayment.entity.User;
import com.p2ppayment.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
    }

    @Test
    void logWalletStatusChange_Success() throws Exception {
        when(objectMapper.writeValueAsString(any(Object.class)))
                .thenReturn("{\"before_status\":\"ACTIVE\",\"after_status\":\"FROZEN\"}");

        auditService.logWalletStatusChange(
                "WALLET_FREEZE",
                10L,
                "ACTIVE",
                "FROZEN",
                testUser,
                "127.0.0.1"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("WALLET_FREEZE", saved.getAction());
        assertEquals("WALLET", saved.getEntityType());
        assertEquals(10L, saved.getEntityId());
        assertEquals(testUser, saved.getActor());
        assertEquals("{\"before_status\":\"ACTIVE\",\"after_status\":\"FROZEN\"}",
                saved.getDetails());
        assertEquals("127.0.0.1", saved.getIpAddress());
    }

    @Test
    void logWalletStatusChange_WhenSerializationFails_UsesFallbackDetails() throws Exception {
        when(objectMapper.writeValueAsString(any(Object.class)))
                .thenThrow(new RuntimeException("Serialization failed"));

        auditService.logWalletStatusChange(
                "WALLET_FREEZE",
                10L,
                "ACTIVE",
                "FROZEN",
                testUser,
                "127.0.0.1"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("WALLET_FREEZE", saved.getAction());
        assertEquals("WALLET", saved.getEntityType());
        assertEquals(10L, saved.getEntityId());
        assertEquals("{\"note\": \"audit details unavailable\"}", saved.getDetails());
        assertEquals("127.0.0.1", saved.getIpAddress());
    }

    @Test
    void logAction_WithIpAddressAndPlainText_WrapsDetailsAsJson() throws Exception {
        when(objectMapper.writeValueAsString(any(Object.class)))
                .thenReturn("{\"note\":\"Searched for users\"}");

        auditService.logAction(
                "USER_SEARCH",
                "USER",
                1L,
                testUser,
                "Searched for users",
                "127.0.0.1"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("USER_SEARCH", saved.getAction());
        assertEquals("USER", saved.getEntityType());
        assertEquals(1L, saved.getEntityId());
        assertEquals(testUser, saved.getActor());
        assertEquals("{\"note\":\"Searched for users\"}", saved.getDetails());
        assertEquals("127.0.0.1", saved.getIpAddress());
    }

    @Test
    void logAction_WithoutIpAddress_DelegatesWithNullIpAddress() throws Exception {
        when(objectMapper.writeValueAsString(any(Object.class)))
                .thenReturn("{\"note\":\"User viewed profile\"}");

        auditService.logAction(
                "PROFILE_VIEW",
                "USER",
                1L,
                testUser,
                "User viewed profile"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("PROFILE_VIEW", saved.getAction());
        assertEquals("{\"note\":\"User viewed profile\"}", saved.getDetails());
        assertNull(saved.getIpAddress());
    }

    @Test
    void logAction_WhenDetailsAlreadyJsonObject_PreservesJson() {
        String json = "  {\"key\":\"value\"}  ";

        auditService.logAction(
                "JSON_ACTION",
                "TEST",
                2L,
                testUser,
                json,
                "127.0.0.1"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        assertEquals("{\"key\":\"value\"}", captor.getValue().getDetails());
        verifyNoInteractions(objectMapper);
    }

    @Test
    void logAction_WhenDetailsAlreadyJsonArray_PreservesJson() {
        String json = "  [1,2,3]  ";

        auditService.logAction(
                "JSON_ARRAY_ACTION",
                "TEST",
                3L,
                testUser,
                json,
                "127.0.0.1"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        assertEquals("[1,2,3]", captor.getValue().getDetails());
        verifyNoInteractions(objectMapper);
    }

    @Test
    void logAction_WhenDetailsAreNull_SavesNullDetails() {
        auditService.logAction(
                "NULL_DETAILS",
                "TEST",
                4L,
                testUser,
                null,
                "127.0.0.1"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        assertNull(captor.getValue().getDetails());
        verifyNoInteractions(objectMapper);
    }

    @Test
    void logAction_WhenJsonSerializationFails_UsesFallbackJson() throws Exception {
        when(objectMapper.writeValueAsString(any(Object.class)))
                .thenThrow(new RuntimeException("Serialization failed"));

        auditService.logAction(
                "FALLBACK_ACTION",
                "TEST",
                5L,
                testUser,
                "Some details",
                "127.0.0.1"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        assertEquals("{\"note\": \"audit details unavailable\"}",
                captor.getValue().getDetails());
    }

    @Test
    void logAction_WithEmptyDetails_WrapsEmptyStringAsJson() throws Exception {
        when(objectMapper.writeValueAsString(any(Object.class)))
                .thenReturn("{\"note\":\"\"}");

        auditService.logAction(
                "EMPTY_DETAILS",
                "TEST",
                6L,
                testUser,
                "",
                "127.0.0.1"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        assertEquals("{\"note\":\"\"}", captor.getValue().getDetails());
    }
}
