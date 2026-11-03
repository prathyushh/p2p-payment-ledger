package com.p2ppayment.controller;

import com.p2ppayment.dto.DailySummaryResponse;
import com.p2ppayment.service.AdminReconciliationService;
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
import java.time.LocalDate;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminReconciliationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminReconciliationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminReconciliationService adminReconciliationService;

    private DailySummaryResponse dailySummaryResponse;

    @BeforeEach
    void setUp() {
        dailySummaryResponse = new DailySummaryResponse(
            1L,
            LocalDate.now(),
            50L,
            new BigDecimal("25000.00"),
            35L,
            new BigDecimal("18000.00"),
            15L,
            new BigDecimal("7000.00"),
            5L,
            new BigDecimal("10000.00"),
            "RECONCILED"
        );
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void listDailySummaries_Success() throws Exception {
        // Arrange
        Page<DailySummaryResponse> page = new PageImpl<>(Arrays.asList(dailySummaryResponse));
        when(adminReconciliationService.listDailySummaries(any(), any(), any()))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/reconciliation/daily-summaries")
                .param("page", "0")
                .param("size", "20")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].totalTransactions").value(50))
                .andExpect(jsonPath("$.content[0].totalVolume").value(25000.00));
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void getDailySummary_Success() throws Exception {
        // Arrange
        when(adminReconciliationService.getDailySummary(any())).thenReturn(dailySummaryResponse);

        // Act & Assert
        mockMvc.perform(get("/api/admin/reconciliation/daily-summaries/2024-01-15")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTransactions").value(50))
                .andExpect(jsonPath("$.status").value("RECONCILED"));
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void listDailySummaries_WithDateRange_Success() throws Exception {
        // Arrange
        Page<DailySummaryResponse> page = new PageImpl<>(Arrays.asList(dailySummaryResponse));
        when(adminReconciliationService.listDailySummaries(any(), any(), any()))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/reconciliation/daily-summaries")
                .param("page", "0")
                .param("size", "20")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-31")
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void listDailySummaries_NonAdmin_ReturnsForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/reconciliation/daily-summaries")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
