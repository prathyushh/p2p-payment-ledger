package com.p2ppayment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminUserResponse {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private String role;
    private BigDecimal walletBalance;
    private String walletStatus;
    private LocalDateTime createdAt;
    private Boolean accountLocked;
    private LocalDateTime lockedUntil;
}
