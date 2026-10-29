package com.p2ppayment.controller;

import com.p2ppayment.dto.LedgerEntryResponse;
import com.p2ppayment.dto.TransactionDetailResponse;
import com.p2ppayment.dto.TransactionResponse;
import com.p2ppayment.entity.LedgerEntry;
import com.p2ppayment.entity.Transaction;
import com.p2ppayment.repository.LedgerEntryRepository;
import com.p2ppayment.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/transactions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTransactionController {

    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public AdminTransactionController(TransactionRepository transactionRepository,
                                     LedgerEntryRepository ledgerEntryRepository) {
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    /**
     * GET /api/admin/transactions
     * List all transactions with optional filters (type, status, amount range, date range).
     */
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> listTransactions(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Transaction> transactions;

        try {
            // Apply filters
            if (type != null && status != null && minAmount != null && maxAmount != null && startDate != null && endDate != null) {
                transactions = transactionRepository.findByTypeStatusAndAmountRange(type, status, minAmount, maxAmount, pageable);
            } else if (type != null && status != null && startDate != null && endDate != null) {
                LocalDate start = LocalDate.parse(startDate);
                LocalDate end = LocalDate.parse(endDate);
                transactions = transactionRepository.findByTypeStatusAndDateRange(type, status, start, end, pageable);
            } else if (type != null && status != null) {
                transactions = transactionRepository.findByTypeAndStatus(type, status, pageable);
            } else if (minAmount != null && maxAmount != null) {
                transactions = transactionRepository.findByAmountRange(minAmount, maxAmount, pageable);
            } else if (startDate != null && endDate != null) {
                LocalDate start = LocalDate.parse(startDate);
                LocalDate end = LocalDate.parse(endDate);
                transactions = transactionRepository.findByDateRange(start, end, pageable);
            } else if (type != null) {
                transactions = transactionRepository.findAllByType(type, pageable);
            } else if (status != null) {
                transactions = transactionRepository.findAllByStatus(status, pageable);
            } else {
                transactions = transactionRepository.findAll(pageable);
            }
        } catch (Exception e) {
            // If filter parsing fails, return all transactions
            transactions = transactionRepository.findAll(pageable);
        }

        Page<TransactionResponse> response = transactions.map(this::mapToTransactionResponse);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/transactions/{id}
     * Get transaction detail with full ledger entries.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionDetailResponse> getTransactionDetail(@PathVariable Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Transaction not found"));

        List<LedgerEntry> ledgerEntries = ledgerEntryRepository.findAll().stream()
                .filter(le -> le.getTransaction().getId().equals(id))
                .collect(Collectors.toList());

        List<LedgerEntryResponse> ledgerResponses = ledgerEntries.stream()
                .map(le -> new LedgerEntryResponse(
                    le.getId(),
                    le.getType().name(),
                    le.getAmount(),
                    le.getBalanceBefore(),
                    le.getBalanceAfter(),
                    le.getCreatedAt()
                ))
                .collect(Collectors.toList());

        TransactionDetailResponse response = new TransactionDetailResponse(
                transaction.getId(),
                transaction.getType().name(),
                transaction.getStatus().name(),
                transaction.getAmount(),
                transaction.getSenderWallet().getUser().getEmail(),
                transaction.getReceiverWallet().getUser().getEmail(),
                transaction.getRemark(),
                transaction.getSettlementDueDate().toString(),
                transaction.getInitiatedAt(),
                transaction.getSettledAt(),
                ledgerResponses
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Convert Transaction to TransactionResponse.
     */
    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getType().name(),
                transaction.getStatus().name(),
                transaction.getAmount(),
                transaction.getSenderWallet().getUser().getEmail(),
                transaction.getReceiverWallet().getUser().getEmail(),
                transaction.getRemark(),
                transaction.getSettlementDueDate().toString(),
                transaction.getInitiatedAt(),
                transaction.getSettledAt()
        );
    }
}
