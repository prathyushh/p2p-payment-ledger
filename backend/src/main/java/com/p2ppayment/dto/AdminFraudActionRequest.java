package com.p2ppayment.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminFraudActionRequest(
    @NotBlank(message = "Notes are required")
    String notes
) {
}