package com.p2ppayment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record WalletTopUpRequest(
        @NotNull(message = "Simulated account ID is required")
        Long accountId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than 0")
        BigDecimal amount
) {
}