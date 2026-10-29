package com.p2ppayment.dto;

import java.math.BigDecimal;

public record WalletTopUpResponse(
        Long transactionId,
        Long walletId,
        BigDecimal amount,
        BigDecimal newBalance,
        String status,
        String message
) {
}