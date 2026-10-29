package com.p2ppayment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LedgerEntryResponse(
    Long id,
    String type,
    BigDecimal amount,
    BigDecimal balanceBefore,
    BigDecimal balanceAfter,
    LocalDateTime createdAt
) {
}