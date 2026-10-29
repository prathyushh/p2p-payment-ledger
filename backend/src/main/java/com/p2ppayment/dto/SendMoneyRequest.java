package com.p2ppayment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SendMoneyRequest(
        @NotBlank(message = "Recipient identifier (email or phone) is required")
        String recipientIdentifier,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than 0")
        BigDecimal amount,

        @NotBlank(message = "Remark is required")
        String remark,

        @NotBlank(message = "PIN is required")
        String pin
) {
}