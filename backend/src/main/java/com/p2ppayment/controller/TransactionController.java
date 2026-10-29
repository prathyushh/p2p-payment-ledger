package com.p2ppayment.controller;

import com.p2ppayment.dto.LedgerEntryResponse;
import com.p2ppayment.dto.TransactionDetailResponse;
import com.p2ppayment.dto.TransactionResponse;
import com.p2ppayment.dto.TransactionStatsResponse;
import com.p2ppayment.entity.LedgerEntry;
import com.p2ppayment.entity.Transaction;
import com.p2ppayment.repository.LedgerEntryRepository;
import com.p2ppayment.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final LedgerEntryRepository ledgerEntryRepository;

    public TransactionController(TransactionService transactionService,
                               LedgerEntryRepository ledgerEntryRepository) {
        this.transactionService = transactionService;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    /**
     * Get user's transaction history with pagination.
     *
     * @param page page number (0-indexed)
     * @param size page size (default 20)
     * @return paginated transaction list
     */
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getTransactionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        Page<Transaction> transactions = transactionService.getTransactionHistory(userId, page, size);
        Page<TransactionResponse> responses = transactions.map(this::toTransactionResponse);

        return ResponseEntity.ok(responses);
    }

    /**
     * Get mini-statement: last 10 transactions (sent + received).
     *
     * @return list of last 10 transactions
     */
    @GetMapping("/mini-statement")
    public ResponseEntity<List<TransactionResponse>> getMiniStatement() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        List<Transaction> transactions = transactionService.getMiniStatement(userId);
        List<TransactionResponse> responses = transactions.stream()
                .map(this::toTransactionResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Get transaction detail with ledger entries.
     *
     * @param transactionId transaction ID
     * @return transaction detail with ledger entries
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionDetailResponse> getTransactionDetail(
            @PathVariable Long transactionId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        Transaction transaction = transactionService.getTransactionDetail(transactionId, userId);

        // Get ledger entries for this transaction
        List<LedgerEntry> entries = ledgerEntryRepository.findAll().stream()
                .filter(e -> e.getTransaction().getId().equals(transactionId))
                .collect(Collectors.toList());

        List<LedgerEntryResponse> entryResponses = entries.stream()
                .map(this::toLedgerEntryResponse)
                .collect(Collectors.toList());

        TransactionDetailResponse response = new TransactionDetailResponse(
                transaction.getId(),
                transaction.getType().toString(),
                transaction.getStatus().toString(),
                transaction.getAmount(),
                transaction.getSenderWallet().getUser().getEmail(),
                transaction.getReceiverWallet().getUser().getEmail(),
                transaction.getRemark(),
                transaction.getSettlementDueDate() != null ? transaction.getSettlementDueDate().toString() : null,
                transaction.getInitiatedAt(),
                transaction.getSettledAt(),
                entryResponses
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get transaction statistics for user.
     *
     * @return transaction statistics (total sent/received, this month's stats)
     */
    @GetMapping("/stats")
    public ResponseEntity<TransactionStatsResponse> getTransactionStats() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        TransactionService.TransactionStats stats = transactionService.getTransactionStats(userId);

        TransactionStatsResponse response = new TransactionStatsResponse(
                stats.totalSent,
                stats.totalReceived,
                stats.totalSentAmount,
                stats.totalReceivedAmount,
                stats.thisMonthSent,
                stats.thisMonthReceived,
                stats.thisMonthSentAmount,
                stats.thisMonthReceivedAmount
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Convert Transaction entity to response DTO.
     */
    private TransactionResponse toTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getType().toString(),
                transaction.getStatus().toString(),
                transaction.getAmount(),
                transaction.getSenderWallet().getUser().getEmail(),
                transaction.getReceiverWallet().getUser().getEmail(),
                transaction.getRemark(),
                transaction.getSettlementDueDate() != null ? transaction.getSettlementDueDate().toString() : null,
                transaction.getInitiatedAt(),
                transaction.getSettledAt()
        );
    }

    /**
     * Convert LedgerEntry entity to response DTO.
     */
    private LedgerEntryResponse toLedgerEntryResponse(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getType().toString(),
                entry.getAmount(),
                entry.getBalanceBefore(),
                entry.getBalanceAfter(),
                entry.getCreatedAt()
        );
    }
}