package com.p2ppayment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2ppayment.dto.AdminFraudActionRequest;
import com.p2ppayment.dto.FraudAlertResponse;
import com.p2ppayment.service.AdminFraudService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminFraudController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminFraudControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminFraudService adminFraudService;

    private FraudAlertResponse fraudAlertResponse;

    @BeforeEach
    void setUp() {
        fraudAlertResponse = new FraudAlertResponse(
            1L,
            1L,
            1L,
            2L,
            "sender@example.com",
            "receiver@example.com",
            new BigDecimal("10000.00"),
            "HIGH_VALUE",
            "PENDING",
            "High value transaction detected",
            null,
            null,
            LocalDateTime.now()
        );
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void listFraudAlerts_Success() throws Exception {
        // Arrange
        Page<FraudAlertResponse> page = new PageImpl<>(Arrays.asList(fraudAlertResponse));
        when(adminFraudService.listFraudAlerts(any(), anyString(), any()))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/fraud/alerts")
                .param("page", "0")
                .param("size", "20")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].alertType").value("HIGH_VALUE"))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void getFraudAlert_Success() throws Exception {
        // Arrange
        when(adminFraudService.getFraudAlert(anyLong())).thenReturn(fraudAlertResponse);

        // Act & Assert
        mockMvc.perform(get("/api/admin/fraud/alerts/1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(10000.00));
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void blockTransaction_Success() throws Exception {
        // Arrange
        AdminFraudActionRequest request = new AdminFraudActionRequest("Suspicious activity detected");
        FraudAlertResponse blockedAlert = new FraudAlertResponse(
            1L, 1L, 1L, 2L, "sender@example.com", "receiver@example.com",
            new BigDecimal("10000.00"), "HIGH_VALUE", "BLOCKED",
            "High value transaction detected", "BLOCKED", "Suspicious activity detected",
            LocalDateTime.now()
        );
        when(adminFraudService.blockTransaction(anyLong(), anyString())).thenReturn(blockedAlert);

        // Act & Assert
        mockMvc.perform(post("/api/admin/fraud/alerts/1/block")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void dismissAlert_Success() throws Exception {
        // Arrange
        AdminFraudActionRequest request = new AdminFraudActionRequest("False positive");
        FraudAlertResponse dismissedAlert = new FraudAlertResponse(
            1L, 1L, 1L, 2L, "sender@example.com", "receiver@example.com",
            new BigDecimal("10000.00"), "HIGH_VALUE", "DISMISSED",
            "High value transaction detected", "DISMISSED", "False positive",
            LocalDateTime.now()
        );
        when(adminFraudService.dismissAlert(anyLong(), anyString())).thenReturn(dismissedAlert);

        // Act & Assert
        mockMvc.perform(post("/api/admin/fraud/alerts/1/dismiss")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"));
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void listFraudAlerts_FilterByStatus_Success() throws Exception {
        // Arrange
        Page<FraudAlertResponse> page = new PageImpl<>(Arrays.asList(fraudAlertResponse));
        when(adminFraudService.listFraudAlerts(any(), anyString(), any()))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/fraud/alerts")
                .param("page", "0")
                .param("size", "20")
                .param("status", "PENDING")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void listFraudAlerts_NonAdmin_ReturnsForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/fraud/alerts")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
