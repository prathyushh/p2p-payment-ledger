package com.p2ppayment.dto;

import jakarta.validation.constraints.NotNull;

public record WalletStatusRequest(
        @NotNull(message = "Action is required")
        String action
) {
}