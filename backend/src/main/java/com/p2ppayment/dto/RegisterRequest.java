package com.p2ppayment.dto;

public record RegisterRequest(
        String name,
        String email,
        String phoneNumber,
        String password,
        String transactionPin
) {
}