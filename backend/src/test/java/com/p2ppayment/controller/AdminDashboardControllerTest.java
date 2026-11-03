package com.p2ppayment.controller;

import com.p2ppayment.dto.AdminDashboardStatsResponse;
import com.p2ppayment.service.AdminDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminDashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminDashboardService adminDashboardService;

    private AdminDashboardStatsResponse dashboardStats;

    @BeforeEach
    void setUp() {
        dashboardStats = new AdminDashboardStatsResponse(
            150L,
            75L,
            15L,
            250L,
            new BigDecimal("125000.00"),
            new BigDecimal("98000.00"),
            125L,
            3L,
            12L,
            5L
        );
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void getDashboardStats_Success() throws Exception {
        // Arrange
        when(adminDashboardService.getDashboardStats()).thenReturn(dashboardStats);

        // Act & Assert
        mockMvc.perform(get("/api/admin/dashboard/stats")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(150))
                .andExpect(jsonPath("$.activeWallets").value(75))
                .andExpect(jsonPath("$.frozenWallets").value(15))
                .andExpect(jsonPath("$.totalTransactions").value(250))
                .andExpect(jsonPath("$.totalVolume").value(125000.00))
                .andExpect(jsonPath("$.pendingFraudAlerts").value(3))
                .andExpect(jsonPath("$.pendingSettlements").value(12));
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void getDashboardStats_NonAdmin_ReturnsForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/dashboard/stats")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
