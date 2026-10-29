package com.p2ppayment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SendMoneyResponse(
        Long transactionId,
        String status,
        BigDecimal amount,
        BigDecimal senderNewBalance,
        LocalDate settlementDueDate,
        String message
) {
}