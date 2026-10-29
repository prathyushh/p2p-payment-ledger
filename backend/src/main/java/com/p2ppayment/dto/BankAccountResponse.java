package com.p2ppayment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BankAccountResponse(
        Long id,
        String bankName,
        String accountNickname,
        String accountIdentifier,
        BigDecimal simulatedBalance,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}