package com.p2ppayment.controller;

import com.p2ppayment.dto.AdminSettlementResponse;
import com.p2ppayment.service.AdminSettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminSettlementController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminSettlementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminSettlementService adminSettlementService;

    private AdminSettlementResponse settlementResponse;

    @BeforeEach
    void setUp() {
        settlementResponse = new AdminSettlementResponse(
            1L,
            1L,
            1L,
            2L,
            "sender@example.com",
            "receiver@example.com",
            new BigDecimal("1000.00"),
            "PENDING",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(3),
            null
        );
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void listSettlementQueue_Success() throws Exception {
        // Arrange
        Page<AdminSettlementResponse> page = new PageImpl<>(Arrays.asList(settlementResponse));
        when(adminSettlementService.listSettlementQueue(any(), anyString()))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/settlements")
                .param("page", "0")
                .param("size", "20")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.content[0].amount").value(1000.00));
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void listSettlementQueue_FilterByStatus_Success() throws Exception {
        // Arrange
        Page<AdminSettlementResponse> page = new PageImpl<>(Arrays.asList(settlementResponse));
        when(adminSettlementService.listSettlementQueue(any(), anyString()))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/settlements")
                .param("page", "0")
                .param("size", "20")
                .param("status", "PENDING")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void listSettlementQueue_SortBySettlementDate_Success() throws Exception {
        // Arrange
        Page<AdminSettlementResponse> page = new PageImpl<>(Arrays.asList(settlementResponse));
        when(adminSettlementService.listSettlementQueue(any(), anyString()))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/settlements")
                .param("page", "0")
                .param("size", "20")
                .param("sortBy", "settlementDate")
                .param("sortDir", "asc")
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void listSettlementQueue_NonAdmin_ReturnsForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/settlements")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
