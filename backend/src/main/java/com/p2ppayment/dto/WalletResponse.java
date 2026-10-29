package com.p2ppayment.dto;

import java.math.BigDecimal;

public record WalletResponse(
        Long id,
        BigDecimal balance,
        BigDecimal dailySpent,
        BigDecimal dailyLimit,
        BigDecimal dailyRemaining,
        String status
) {
}