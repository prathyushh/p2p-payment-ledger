package com.p2ppayment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SettlementQueueResponse {
    private Long overduePendingCount;
    private BigDecimal overdueAmount;
    private Long dueTodayCount;
    private BigDecimal dueTodayAmount;
    private Long dueTomorrowCount;
    private BigDecimal dueTomorrowAmount;
    private Long totalPendingCount;
    private BigDecimal totalPendingAmount;
}
