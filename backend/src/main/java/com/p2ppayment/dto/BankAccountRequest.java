package com.p2ppayment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BankAccountRequest(
        @NotBlank(message = "Bank name is required")
        String bankName,

        @NotBlank(message = "Account nickname is required")
        String accountNickname
) {
}