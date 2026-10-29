package com.p2ppayment.dto;

public record AuthRequest(String email, String password, String ipAddress) {
    public AuthRequest(String email, String password) {
        this(email, password, null);
    }
}