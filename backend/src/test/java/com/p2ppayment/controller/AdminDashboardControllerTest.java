
package com.p2ppayment.controller;

import com.p2ppayment.dto.AdminDashboardStatsResponse;
import com.p2ppayment.entity.DailySummary;
import com.p2ppayment.repository.DailySummaryRepository;
import com.p2ppayment.repository.FraudAlertRepository;
import com.p2ppayment.repository.TransactionRepository;
import com.p2ppayment.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminDashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private TransactionRepository transactionRepository;

    @MockBean
    private FraudAlertRepository fraudAlertRepository;

    @MockBean
    private DailySummaryRepository dailySummaryRepository;

    @Test
    void getDashboardStats_Success() throws Exception {
        LocalDate today = LocalDate.now();

        when(userRepository.count()).thenReturn(150L);

        when(transactionRepository.countInitiatedForDate(any(LocalDate.class)))
                .thenReturn(15L);

        when(transactionRepository.countSettledForDate(any(LocalDate.class)))
                .thenReturn(10L);

        when(transactionRepository.sumVolumeForDate(any(LocalDate.class)))
                .thenReturn(new BigDecimal("125000.00"));

        when(transactionRepository.sumPendingSettlementAmount())
                .thenReturn(new BigDecimal("98000.00"));

        when(fraudAlertRepository.countPendingAlerts())
                .thenReturn(3L);

        when(dailySummaryRepository.findBySummaryDate(any(LocalDate.class)))
                .thenReturn(Optional.empty());

        when(transactionRepository.countSettledTransactionsForDate(any(LocalDate.class)))
                .thenReturn(5L);

        when(transactionRepository.sumSettledAmountForDate(any(LocalDate.class)))
                .thenReturn(new BigDecimal("1000.00"));

        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(150))
                .andExpect(jsonPath("$.todayTransfersInitiated").value(15))
                .andExpect(jsonPath("$.todayTransfersSettled").value(10))
                .andExpect(jsonPath("$.todayTransactionVolume").value(125000.00))
                .andExpect(jsonPath("$.totalPendingSettlementAmount").value(98000.00))
                .andExpect(jsonPath("$.pendingFraudAlertsCount").value(3))
                .andExpect(jsonPath("$.last30DaysDailyVolume").isArray())
                .andExpect(jsonPath("$.last30DaysDailyVolume.length()").value(30))
                .andExpect(jsonPath("$.last30DaysDailyVolume[29].date")
                        .value(today.toString()))
                .andExpect(jsonPath("$.last30DaysDailyVolume[29].volume")
                        .value(1000.00))
                .andExpect(jsonPath("$.last30DaysDailyVolume[29].transactionCount")
                        .value(5));

        verify(userRepository).count();
        verify(transactionRepository).countInitiatedForDate(today);
        verify(transactionRepository).countSettledForDate(today);
        verify(transactionRepository).sumVolumeForDate(today);
        verify(fraudAlertRepository).countPendingAlerts();
        verify(dailySummaryRepository, times(30))
                .findBySummaryDate(any(LocalDate.class));
    }

    @Test
    void getDashboardStats_NullRepositoryValues_UsesDefaults() throws Exception {
        when(userRepository.count()).thenReturn(0L);

        when(transactionRepository.countInitiatedForDate(any(LocalDate.class)))
                .thenReturn(null);

        when(transactionRepository.countSettledForDate(any(LocalDate.class)))
                .thenReturn(null);

        when(transactionRepository.sumVolumeForDate(any(LocalDate.class)))
                .thenReturn(null);

        when(transactionRepository.sumPendingSettlementAmount())
                .thenReturn(null);

        when(fraudAlertRepository.countPendingAlerts())
                .thenReturn(null);

        when(dailySummaryRepository.findBySummaryDate(any(LocalDate.class)))
                .thenReturn(Optional.empty());

        when(transactionRepository.countSettledTransactionsForDate(any(LocalDate.class)))
                .thenReturn(null);

        when(transactionRepository.sumSettledAmountForDate(any(LocalDate.class)))
                .thenReturn(null);

        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(0))
                .andExpect(jsonPath("$.todayTransfersInitiated").value(0))
                .andExpect(jsonPath("$.todayTransfersSettled").value(0))
                .andExpect(jsonPath("$.todayTransactionVolume").value(0))
                .andExpect(jsonPath("$.totalPendingSettlementAmount").value(0))
                .andExpect(jsonPath("$.pendingFraudAlertsCount").value(0))
                .andExpect(jsonPath("$.last30DaysDailyVolume.length()").value(30))
                .andExpect(jsonPath("$.last30DaysDailyVolume[29].volume").value(0))
                .andExpect(jsonPath("$.last30DaysDailyVolume[29].transactionCount").value(0));
    }

    @Test
    void getDashboardStats_UsesDailySummaryWhenAvailable() throws Exception {
        LocalDate today = LocalDate.now();

        DailySummary summary = mock(DailySummary.class);

        when(summary.getTotalTransfersSettledAmount())
                .thenReturn(new BigDecimal("7500.00"));

        when(summary.getTotalTransfersSettled())
        .thenReturn(12);

        when(userRepository.count()).thenReturn(100L);

        when(transactionRepository.countInitiatedForDate(any(LocalDate.class)))
                .thenReturn(20L);

        when(transactionRepository.countSettledForDate(any(LocalDate.class)))
                .thenReturn(18L);

        when(transactionRepository.sumVolumeForDate(any(LocalDate.class)))
                .thenReturn(new BigDecimal("15000.00"));

        when(transactionRepository.sumPendingSettlementAmount())
                .thenReturn(new BigDecimal("2000.00"));

        when(fraudAlertRepository.countPendingAlerts())
                .thenReturn(2L);

        when(dailySummaryRepository.findBySummaryDate(any(LocalDate.class)))
                .thenReturn(Optional.empty());

        when(dailySummaryRepository.findBySummaryDate(today))
                .thenReturn(Optional.of(summary));

        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.last30DaysDailyVolume.length()").value(30))
                .andExpect(jsonPath("$.last30DaysDailyVolume[29].date")
                        .value(today.toString()))
                .andExpect(jsonPath("$.last30DaysDailyVolume[29].volume")
                        .value(7500.00))
                .andExpect(jsonPath("$.last30DaysDailyVolume[29].transactionCount")
                        .value(12));

        verify(transactionRepository, never())
                .countSettledTransactionsForDate(today);

        verify(transactionRepository, never())
                .sumSettledAmountForDate(today);
    }

    @Test
    void getDashboardStats_DailySummaryLookupFailure_FallsBackToLiveData()
            throws Exception {

        LocalDate today = LocalDate.now();

        when(userRepository.count()).thenReturn(50L);

        when(transactionRepository.countInitiatedForDate(any(LocalDate.class)))
                .thenReturn(10L);

        when(transactionRepository.countSettledForDate(any(LocalDate.class)))
                .thenReturn(8L);

        when(transactionRepository.sumVolumeForDate(any(LocalDate.class)))
                .thenReturn(new BigDecimal("5000.00"));

        when(transactionRepository.sumPendingSettlementAmount())
                .thenReturn(BigDecimal.ZERO);

        when(fraudAlertRepository.countPendingAlerts())
                .thenReturn(0L);

        when(dailySummaryRepository.findBySummaryDate(any(LocalDate.class)))
                .thenThrow(new RuntimeException("Summary database unavailable"));

        when(transactionRepository.countSettledTransactionsForDate(any(LocalDate.class)))
                .thenReturn(4L);

        when(transactionRepository.sumSettledAmountForDate(any(LocalDate.class)))
                .thenReturn(new BigDecimal("2500.00"));

        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.last30DaysDailyVolume.length()").value(30))
                .andExpect(jsonPath("$.last30DaysDailyVolume[29].date")
                        .value(today.toString()))
                .andExpect(jsonPath("$.last30DaysDailyVolume[29].volume")
                        .value(2500.00))
                .andExpect(jsonPath("$.last30DaysDailyVolume[29].transactionCount")
                        .value(4));
    }
}