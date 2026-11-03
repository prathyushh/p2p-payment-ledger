
package com.p2ppayment.controller;

import com.p2ppayment.entity.DailySummary;
import com.p2ppayment.repository.DailySummaryRepository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminReconciliationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminReconciliationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DailySummaryRepository dailySummaryRepository;

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void getTodayReconciliation_NoSummary_ReturnsDefaultSummary()
            throws Exception {

        LocalDate today = LocalDate.now();

        when(dailySummaryRepository.findBySummaryDate(today))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/reconciliation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summaryDate").value(today.toString()))
                .andExpect(jsonPath("$.totalTransfersInitiated").value(0))
                .andExpect(jsonPath("$.totalTransfersInitiatedAmount").value(0))
                .andExpect(jsonPath("$.totalTransfersSettled").value(0))
                .andExpect(jsonPath("$.totalTransfersSettledAmount").value(0))
                .andExpect(jsonPath("$.fraudAlertsRaised").value(0))
                .andExpect(jsonPath("$.newUserRegistrations").value(0))
                .andExpect(jsonPath("$.totalPendingSettlementAmount").value(0))
                .andExpect(jsonPath("$.reconciliationDebitSum").value(0))
                .andExpect(jsonPath("$.reconciliationCreditSum").value(0))
                .andExpect(jsonPath("$.reconciliationMismatch").value(false));

        verify(dailySummaryRepository).findBySummaryDate(today);
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void getTodayReconciliation_SummaryExists_ReturnsSummary()
            throws Exception {

        LocalDate today = LocalDate.now();
        DailySummary summary = createDailySummary(today);

        when(dailySummaryRepository.findBySummaryDate(today))
                .thenReturn(Optional.of(summary));

        mockMvc.perform(get("/api/admin/reconciliation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.summaryDate").value(today.toString()))
                .andExpect(jsonPath("$.totalTransfersInitiated").value(50))
                .andExpect(jsonPath("$.totalTransfersInitiatedAmount").value(10000.00))
                .andExpect(jsonPath("$.totalTransfersSettled").value(35))
                .andExpect(jsonPath("$.totalTransfersSettledAmount").value(7000.00))
                .andExpect(jsonPath("$.fraudAlertsRaised").value(5))
                .andExpect(jsonPath("$.newUserRegistrations").value(10))
                .andExpect(jsonPath("$.totalPendingSettlementAmount").value(3000.00))
                .andExpect(jsonPath("$.reconciliationDebitSum").value(10000.00))
                .andExpect(jsonPath("$.reconciliationCreditSum").value(7000.00))
                .andExpect(jsonPath("$.reconciliationMismatch").value(false));

        verify(dailySummaryRepository).findBySummaryDate(today);
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void getReconciliationByDate_NoSummary_ReturnsDefaultSummary()
            throws Exception {

        LocalDate date = LocalDate.of(2024, 1, 15);

        when(dailySummaryRepository.findBySummaryDate(date))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/reconciliation/2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summaryDate").value("2024-01-15"))
                .andExpect(jsonPath("$.totalTransfersInitiated").value(0))
                .andExpect(jsonPath("$.totalTransfersInitiatedAmount").value(0))
                .andExpect(jsonPath("$.totalTransfersSettled").value(0))
                .andExpect(jsonPath("$.totalTransfersSettledAmount").value(0))
                .andExpect(jsonPath("$.fraudAlertsRaised").value(0))
                .andExpect(jsonPath("$.newUserRegistrations").value(0))
                .andExpect(jsonPath("$.totalPendingSettlementAmount").value(0))
                .andExpect(jsonPath("$.reconciliationDebitSum").value(0))
                .andExpect(jsonPath("$.reconciliationCreditSum").value(0))
                .andExpect(jsonPath("$.reconciliationMismatch").value(false));

        verify(dailySummaryRepository).findBySummaryDate(date);
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void getReconciliationByDate_InvalidDate_ReturnsBadRequest()
            throws Exception {

        mockMvc.perform(get("/api/admin/reconciliation/invalid-date"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(dailySummaryRepository);
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void getReconciliationByDate_SummaryExists_ReturnsSummary()
            throws Exception {

        LocalDate date = LocalDate.of(2024, 1, 15);
        DailySummary summary = createDailySummary(date);

        when(dailySummaryRepository.findBySummaryDate(date))
                .thenReturn(Optional.of(summary));

        mockMvc.perform(get("/api/admin/reconciliation/2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.summaryDate").value("2024-01-15"))
                .andExpect(jsonPath("$.totalTransfersInitiated").value(50))
                .andExpect(jsonPath("$.totalTransfersSettled").value(35))
                .andExpect(jsonPath("$.fraudAlertsRaised").value(5))
                .andExpect(jsonPath("$.newUserRegistrations").value(10))
                .andExpect(jsonPath("$.reconciliationMismatch").value(false));

        verify(dailySummaryRepository).findBySummaryDate(date);
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void getTodayReconciliation_NonAdmin() throws Exception {

        // Filters are disabled, so this test does not verify role enforcement.
        when(dailySummaryRepository.findBySummaryDate(LocalDate.now()))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/reconciliation"))
                .andExpect(status().isOk());
    }

    private DailySummary createDailySummary(LocalDate date) {
        DailySummary summary = mock(DailySummary.class);

        when(summary.getId()).thenReturn(1L);
        when(summary.getSummaryDate()).thenReturn(date);
        when(summary.getTotalTransfersInitiated()).thenReturn(50);
        when(summary.getTotalTransfersInitiatedAmount())
                .thenReturn(new BigDecimal("10000.00"));
        when(summary.getTotalTransfersSettled()).thenReturn(35);
        when(summary.getTotalTransfersSettledAmount())
                .thenReturn(new BigDecimal("7000.00"));
        when(summary.getFraudAlertsRaised()).thenReturn(5);
        when(summary.getNewUserRegistrations()).thenReturn(10);
        when(summary.getTotalPendingSettlementAmount())
                .thenReturn(new BigDecimal("3000.00"));
        when(summary.getReconciliationDebitSum())
                .thenReturn(new BigDecimal("10000.00"));
        when(summary.getReconciliationCreditSum())
                .thenReturn(new BigDecimal("7000.00"));
        when(summary.getReconciliationMismatch()).thenReturn(false);
        when(summary.getCreatedAt()).thenReturn(LocalDateTime.now());

        return summary;
    }
}
