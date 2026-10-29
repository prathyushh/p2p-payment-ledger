package com.p2ppayment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminSettlementResponse {
    private Long id;
    private String type;
    private String status;
    private BigDecimal amount;
    private String senderEmail;
    private String receiverEmail;
    private String remark;
    private LocalDate settlementDueDate;
    private Boolean isOverdue;
    private LocalDateTime initiatedAt;
    private LocalDateTime settledAt;
}
