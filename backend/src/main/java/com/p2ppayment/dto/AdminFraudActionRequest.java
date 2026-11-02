package com.p2ppayment.dto;

public record AdminFraudActionRequest(
    String notes  // Optional - can be null or empty
) {
}