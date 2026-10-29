package com.p2ppayment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminDashboardStatsResponse {
    private Long totalUsers;
    private Long todayTransfersInitiated;
    private Long todayTransfersSettled;
    private BigDecimal todayTransactionVolume;
    private BigDecimal totalPendingSettlementAmount;
    private Long pendingFraudAlertsCount;
    private List<DailyVolumeData> last30DaysDailyVolume;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyVolumeData {
        private String date;
        private BigDecimal volume;
        private Long transactionCount;
    }
}
