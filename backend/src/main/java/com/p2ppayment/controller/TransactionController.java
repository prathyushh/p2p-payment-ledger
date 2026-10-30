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
     * Extract authenticated user ID from security context.
     */
    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid user ID in token");
        }
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

        Long userId = getAuthenticatedUserId();

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
        Long userId = getAuthenticatedUserId();

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

        Long userId = getAuthenticatedUserId();

        Transaction transaction = transactionService.getTransactionDetail(transactionId, userId);

        // Get ledger entries for this transaction
        List<LedgerEntry> entries = ledgerEntryRepository.findAll().stream()
                .filter(e -> e.getTransaction().getId().equals(transactionId))
                .collect(Collectors.toList());

        List<LedgerEntryResponse> entryResponses = entries.stream()
                .map(this::toLedgerEntryResponse)
                .collect(Collectors.toList());

        // Safely get sender and receiver emails (handle null wallets)
        String senderEmail = transaction.getSenderWallet() != null && transaction.getSenderWallet().getUser() != null
                ? transaction.getSenderWallet().getUser().getEmail()
                : "System";
        String receiverEmail = transaction.getReceiverWallet() != null && transaction.getReceiverWallet().getUser() != null
                ? transaction.getReceiverWallet().getUser().getEmail()
                : "System";

        TransactionDetailResponse response = new TransactionDetailResponse(
                transaction.getId(),
                transaction.getType().toString(),
                transaction.getStatus().toString(),
                transaction.getAmount(),
                senderEmail,
                receiverEmail,
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
        Long userId = getAuthenticatedUserId();

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
     * Convert Transaction entity to response DTO with null-safety.
     * Handles cases where sender or receiver wallet may be null (e.g., system transactions).
     */
    private TransactionResponse toTransactionResponse(Transaction transaction) {
        // Safely extract sender email (handle null wallet)
        String senderEmail = transaction.getSenderWallet() != null && transaction.getSenderWallet().getUser() != null
                ? transaction.getSenderWallet().getUser().getEmail()
                : "System";

        // Safely extract receiver email (handle null wallet)
        String receiverEmail = transaction.getReceiverWallet() != null && transaction.getReceiverWallet().getUser() != null
                ? transaction.getReceiverWallet().getUser().getEmail()
                : "System";

        return new TransactionResponse(
                transaction.getId(),
                transaction.getType().toString(),
                transaction.getStatus().toString(),
                transaction.getAmount(),
                senderEmail,
                receiverEmail,
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



