package com.p2ppayment.dto;

public record FraudAlertResponse(
    Long id,
    Long transactionId,
    String alertType,
    String status,
    String notes,
    String createdAt
) {
}