package com.p2ppayment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    @NotBlank(message = "Password is required")
    String password,
    
    String ipAddress
) {
    public AuthRequest(String email, String password) {
        this(email, password, null);
    }
}
