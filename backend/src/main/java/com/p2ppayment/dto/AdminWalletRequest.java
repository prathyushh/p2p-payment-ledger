package com.p2ppayment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AdminWalletRequest(
        @NotNull(message = "Wallet ID is required")
        @Positive(message = "Wallet ID must be positive")
        Long walletId,

        @NotNull(message = "Action is required")
        String action
) {
}