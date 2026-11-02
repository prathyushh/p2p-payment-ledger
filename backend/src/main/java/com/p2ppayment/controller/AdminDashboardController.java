package com.p2ppayment.controller;

import com.p2ppayment.dto.AdminDashboardStatsResponse;
import com.p2ppayment.repository.FraudAlertRepository;
import com.p2ppayment.repository.TransactionRepository;
import com.p2ppayment.repository.UserRepository;
import com.p2ppayment.repository.DailySummaryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final DailySummaryRepository dailySummaryRepository;

    public AdminDashboardController(UserRepository userRepository,
                                  TransactionRepository transactionRepository,
                                  FraudAlertRepository fraudAlertRepository,
                                  DailySummaryRepository dailySummaryRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.dailySummaryRepository = dailySummaryRepository;
    }

    /**
     * GET /api/admin/dashboard/stats
     * Returns dashboard statistics for admin panel.
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminDashboardStatsResponse> getDashboardStats() {
        LocalDate today = LocalDate.now();
        
        // Get counts and totals for today
        Long totalUsers = userRepository.count();
        Long todayInitiated = transactionRepository.countInitiatedForDate(today);
        Long todaySettled = transactionRepository.countSettledForDate(today);
        BigDecimal todayVolume = transactionRepository.sumVolumeForDate(today);
        BigDecimal pendingSettlementAmount = transactionRepository.sumPendingSettlementAmount();
        Long pendingFraudAlerts = fraudAlertRepository.countPendingAlerts();

        // Get last 30 days daily volume - ALWAYS show all 30 days with live data
        List<AdminDashboardStatsResponse.DailyVolumeData> last30Days = new ArrayList<>();
        LocalDate endDate = today;
        
        for (int i = 29; i >= 0; i--) {  // Start from 29 days ago to today
            LocalDate date = endDate.minusDays(i);
            
            // Try to get from daily_summary first (for historical accuracy)
            boolean foundInSummary = false;
            try {
                var summaryOpt = dailySummaryRepository.findBySummaryDate(date);
                if (summaryOpt.isPresent()) {
                    var summary = summaryOpt.get();
                    last30Days.add(AdminDashboardStatsResponse.DailyVolumeData.builder()
                            .date(date.toString())
                            .volume(summary.getTotalTransfersSettledAmount())
                            .transactionCount((long) summary.getTotalTransfersSettled())
                            .build());
                    foundInSummary = true;
                }
            } catch (Exception e) {
                // Continue to live calculation
            }
            
            // If not in summary, calculate from live transaction data
            if (!foundInSummary) {
                Long settledCount = transactionRepository.countSettledTransactionsForDate(date);
                BigDecimal settledAmount = transactionRepository.sumSettledAmountForDate(date);
                
                last30Days.add(AdminDashboardStatsResponse.DailyVolumeData.builder()
                        .date(date.toString())
                        .volume(settledAmount != null ? settledAmount : BigDecimal.ZERO)
                        .transactionCount(settledCount != null ? settledCount : 0L)
                        .build());
            }
        }

        AdminDashboardStatsResponse response = AdminDashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .todayTransfersInitiated(todayInitiated != null ? todayInitiated : 0L)
                .todayTransfersSettled(todaySettled != null ? todaySettled : 0L)
                .todayTransactionVolume(todayVolume != null ? todayVolume : BigDecimal.ZERO)
                .totalPendingSettlementAmount(pendingSettlementAmount != null ? pendingSettlementAmount : BigDecimal.ZERO)
                .pendingFraudAlertsCount(pendingFraudAlerts != null ? pendingFraudAlerts : 0L)
                .last30DaysDailyVolume(last30Days)
                .build();

        return ResponseEntity.ok(response);
    }
}
