package com.p2ppayment.dto;

import java.math.BigDecimal;

public record TransactionStatsResponse(
    Long totalSent,
    Long totalReceived,
    BigDecimal totalSentAmount,
    BigDecimal totalReceivedAmount,
    Long thisMonthSentCount,
    Long thisMonthReceivedCount,
    BigDecimal thisMonthSentAmount,
    BigDecimal thisMonthReceivedAmount
) {
}