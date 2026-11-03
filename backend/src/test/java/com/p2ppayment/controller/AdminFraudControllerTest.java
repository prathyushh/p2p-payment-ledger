
package com.p2ppayment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2ppayment.dto.AdminFraudActionRequest;
import com.p2ppayment.entity.FraudAlert;
import com.p2ppayment.entity.Transaction;
import com.p2ppayment.repository.FraudAlertRepository;
import com.p2ppayment.repository.UserRepository;
import com.p2ppayment.service.AuditService;
import com.p2ppayment.service.FraudService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminFraudController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminFraudControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FraudService fraudService;

    @MockBean
    private FraudAlertRepository fraudAlertRepository;

    @MockBean
    private AuditService auditService;

    @MockBean
    private UserRepository userRepository;

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "1",
                        null,
                        Collections.emptyList()
                )
        );

        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllAlerts_Success() throws Exception {
        FraudAlert alert = createFraudAlert(1L);

        when(fraudAlertRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(alert));

        mockMvc.perform(get("/api/admin/fraud"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].transactionId").value(100))
                .andExpect(jsonPath("$[0].alertType").value("HIGH_VALUE"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].transactionStatus").value("PENDING"));

        verify(fraudAlertRepository)
                .findAllByOrderByCreatedAtDesc();
    }

    @Test
    void blockAlert_Success() throws Exception {
        Long alertId = 1L;

        AdminFraudActionRequest request =
                new AdminFraudActionRequest("Suspicious activity");

        FraudAlert alert = createFraudAlert(alertId);
        alert.setStatus(FraudAlert.Status.BLOCKED);
        alert.setNotes(request.notes());

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        when(fraudService.blockAlert(alertId, request.notes()))
                .thenReturn(alert);

        mockMvc.perform(put("/api/admin/fraud/{alertId}/block", alertId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alertId))
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.notes").value("Suspicious activity"))
                .andExpect(jsonPath("$.alertType").value("HIGH_VALUE"));

        verify(fraudService)
                .blockAlert(alertId, request.notes());

        verify(auditService).logAction(
                eq("FRAUD_ALERT_BLOCKED"),
                eq("FRAUD_ALERT"),
                eq(alertId),
                isNull(),
                eq("Alert blocked with notes: " + request.notes()),
                eq("127.0.0.1")
        );
    }

    @Test
    void dismissAlert_Success() throws Exception {
        Long alertId = 1L;

        AdminFraudActionRequest request =
                new AdminFraudActionRequest(
                        "Reviewed and found legitimate"
                );

        FraudAlert alert = createFraudAlert(alertId);
        alert.setStatus(FraudAlert.Status.DISMISSED);
        alert.setNotes(request.notes());

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        when(fraudService.dismissAlert(alertId, request.notes()))
                .thenReturn(alert);

        mockMvc.perform(put("/api/admin/fraud/{alertId}/dismiss", alertId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alertId))
                .andExpect(jsonPath("$.status").value("DISMISSED"))
                .andExpect(jsonPath("$.notes")
                        .value("Reviewed and found legitimate"))
                .andExpect(jsonPath("$.alertType").value("HIGH_VALUE"));

        verify(fraudService)
                .dismissAlert(alertId, request.notes());

        verify(auditService).logAction(
                eq("FRAUD_ALERT_DISMISSED"),
                eq("FRAUD_ALERT"),
                eq(alertId),
                isNull(),
                eq("Alert dismissed with notes: " + request.notes()),
                eq("127.0.0.1")
        );
    }

    @Test
    void blockAlert_WithXForwardedFor_UsesFirstIpAddress()
            throws Exception {

        Long alertId = 1L;

        AdminFraudActionRequest request =
                new AdminFraudActionRequest("Suspicious activity");

        FraudAlert alert = createFraudAlert(alertId);
        alert.setStatus(FraudAlert.Status.BLOCKED);
        alert.setNotes(request.notes());

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        when(fraudService.blockAlert(alertId, request.notes()))
                .thenReturn(alert);

        mockMvc.perform(put("/api/admin/fraud/{alertId}/block", alertId)
                        .header(
                                "X-Forwarded-For",
                                "192.168.1.10, 10.0.0.1"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        verify(auditService).logAction(
                eq("FRAUD_ALERT_BLOCKED"),
                eq("FRAUD_ALERT"),
                eq(alertId),
                isNull(),
                eq("Alert blocked with notes: " + request.notes()),
                eq("192.168.1.10")
        );
    }

    @Test
    void dismissAlert_WithXForwardedFor_UsesFirstIpAddress()
            throws Exception {

        Long alertId = 1L;

        AdminFraudActionRequest request =
                new AdminFraudActionRequest("Reviewed");

        FraudAlert alert = createFraudAlert(alertId);
        alert.setStatus(FraudAlert.Status.DISMISSED);
        alert.setNotes(request.notes());

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        when(fraudService.dismissAlert(alertId, request.notes()))
                .thenReturn(alert);

        mockMvc.perform(put("/api/admin/fraud/{alertId}/dismiss", alertId)
                        .header(
                                "X-Forwarded-For",
                                "192.168.1.20, 10.0.0.2"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"));

        verify(auditService).logAction(
                eq("FRAUD_ALERT_DISMISSED"),
                eq("FRAUD_ALERT"),
                eq(alertId),
                isNull(),
                eq("Alert dismissed with notes: " + request.notes()),
                eq("192.168.1.20")
        );
    }

    private FraudAlert createFraudAlert(Long alertId) {
        Transaction transaction = new Transaction();
        transaction.setId(100L);
        transaction.setStatus(Transaction.Status.PENDING);

        return FraudAlert.builder()
                .id(alertId)
                .transaction(transaction)
                .alertType(FraudAlert.AlertType.HIGH_VALUE)
                .status(FraudAlert.Status.PENDING)
                .notes("Test fraud alert")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
