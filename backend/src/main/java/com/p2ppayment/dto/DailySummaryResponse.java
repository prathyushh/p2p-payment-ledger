package com.p2ppayment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DailySummaryResponse {
    private Long id;
    private LocalDate summaryDate;
    private Integer totalTransfersInitiated;
    private BigDecimal totalTransfersInitiatedAmount;
    private Integer totalTransfersSettled;
    private BigDecimal totalTransfersSettledAmount;
    private Integer fraudAlertsRaised;
    private Integer newUserRegistrations;
    private BigDecimal totalPendingSettlementAmount;
    private BigDecimal reconciliationDebitSum;
    private BigDecimal reconciliationCreditSum;
    private Boolean reconciliationMismatch;
    private LocalDateTime createdAt;
}