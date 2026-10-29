package com.p2ppayment.dto;

public record RegisterResponse(String token, Long id, String name, String email, String role) {
}