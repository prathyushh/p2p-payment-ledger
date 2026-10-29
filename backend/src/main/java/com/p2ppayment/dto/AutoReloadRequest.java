package com.p2ppayment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AutoReloadRequest(
        @NotNull(message = "isEnabled is required")
        Boolean isEnabled,

        Long accountId,

        @Positive(message = "thresholdAmount must be greater than 0")
        BigDecimal thresholdAmount,

        @Positive(message = "reloadAmount must be greater than 0")
        BigDecimal reloadAmount
) {
}