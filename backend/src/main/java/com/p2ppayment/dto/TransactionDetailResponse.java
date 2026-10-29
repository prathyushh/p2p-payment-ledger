package com.p2ppayment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionDetailResponse(
    Long id,
    String type,
    String status,
    BigDecimal amount,
    String senderEmail,
    String receiverEmail,
    String remark,
    String settlementDueDate,
    LocalDateTime initiatedAt,
    LocalDateTime settledAt,
    java.util.List<LedgerEntryResponse> ledgerEntries
) {
}