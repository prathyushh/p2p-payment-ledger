package com.p2ppayment.dto;

public record RecipientSearchResponse(
        Long id,
        String name,
        String email,
        String phoneNumber
) {}
