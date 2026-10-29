package com.p2ppayment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AutoReloadResponse(
        Long id,
        Long walletId,
        Boolean isEnabled,
        Long accountId,
        String accountNickname,
        BigDecimal thresholdAmount,
        BigDecimal reloadAmount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}