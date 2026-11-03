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
     * List all transactions with optional filters (type, status, amount range, date range) and sorting.
     */
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> listTransactions(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "initiatedAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        // Map frontend sort field names to entity field names
        String entitySortField = mapSortField(sortBy);
        
        // Create pageable with sorting
        org.springframework.data.domain.Sort.Direction direction = 
            sortDir.equalsIgnoreCase("asc") ? 
            org.springframework.data.domain.Sort.Direction.ASC : 
            org.springframework.data.domain.Sort.Direction.DESC;
        
        Pageable pageable = PageRequest.of(
            page, 
            Math.min(size, 100),
            org.springframework.data.domain.Sort.by(direction, entitySortField)
        );
        
        Page<Transaction> transactions;

        try {
            // Parse date range if provided
            LocalDate start = null;
            LocalDate end = null;
            if (startDate != null && !startDate.isEmpty()) {
                start = LocalDate.parse(startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                end = LocalDate.parse(endDate);
            }

            // Build query dynamically based on provided filters
            boolean hasType = type != null && !type.isEmpty();
            boolean hasStatus = status != null && !status.isEmpty();
            boolean hasAmountRange = minAmount != null && maxAmount != null;
            boolean hasDateRange = start != null && end != null;

            // All four filters
            if (hasType && hasStatus && hasAmountRange && hasDateRange) {
                transactions = transactionRepository.findByAllFilters(
                    type, status, minAmount, maxAmount, start, end, pageable);
            }
            // Type + Status + Date
            else if (hasType && hasStatus && hasDateRange) {
                transactions = transactionRepository.findByTypeStatusAndDateRange(
                    type, status, start, end, pageable);
            }
            // Type + Status + Amount
            else if (hasType && hasStatus && hasAmountRange) {
                transactions = transactionRepository.findByTypeStatusAndAmountRange(
                    type, status, minAmount, maxAmount, pageable);
            }
            // Status + Amount + Date
            else if (hasStatus && hasAmountRange && hasDateRange) {
                transactions = transactionRepository.findByStatusAmountAndDateRange(
                    status, minAmount, maxAmount, start, end, pageable);
            }
            // Type + Amount + Date
            else if (hasType && hasAmountRange && hasDateRange) {
                transactions = transactionRepository.findByTypeAmountAndDateRange(
                    type, minAmount, maxAmount, start, end, pageable);
            }
            // Type + Status
            else if (hasType && hasStatus) {
                transactions = transactionRepository.findByTypeAndStatus(type, status, pageable);
            }
            // Type + Amount
            else if (hasType && hasAmountRange) {
                transactions = transactionRepository.findByTypeAndAmountRange(
                    type, minAmount, maxAmount, pageable);
            }
            // Type + Date
            else if (hasType && hasDateRange) {
                transactions = transactionRepository.findByTypeAndDateRange(type, start, end, pageable);
            }
            // Status + Amount
            else if (hasStatus && hasAmountRange) {
                transactions = transactionRepository.findByStatusAndAmountRange(
                    status, minAmount, maxAmount, pageable);
            }
            // Status + Date
            else if (hasStatus && hasDateRange) {
                transactions = transactionRepository.findByStatusAndDateRange(
                    status, start, end, pageable);
            }
            // Amount + Date
            else if (hasAmountRange && hasDateRange) {
                transactions = transactionRepository.findByAmountAndDateRange(
                    minAmount, maxAmount, start, end, pageable);
            }
            // Single filters
            else if (hasType) {
                transactions = transactionRepository.findAllByType(type, pageable);
            }
            else if (hasStatus) {
                transactions = transactionRepository.findAllByStatus(status, pageable);
            }
            else if (hasAmountRange) {
                transactions = transactionRepository.findByAmountRange(minAmount, maxAmount, pageable);
            }
            else if (hasDateRange) {
                transactions = transactionRepository.findByDateRange(start, end, pageable);
            }
            // No filters
            else {
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

        // Handle null fields safely
        String senderEmail = transaction.getSenderWallet() != null
            ? transaction.getSenderWallet().getUser().getEmail()
            : "System";

        String receiverEmail = transaction.getReceiverWallet() != null
            ? transaction.getReceiverWallet().getUser().getEmail()
            : "Unknown";

        String settlementDueDate = transaction.getSettlementDueDate() != null
            ? transaction.getSettlementDueDate().toString()
            : null;

        TransactionDetailResponse response = new TransactionDetailResponse(
                transaction.getId(),
                transaction.getType().name(),
                transaction.getStatus().name(),
                transaction.getAmount(),
                senderEmail,
                receiverEmail,
                transaction.getRemark(),
                settlementDueDate,
                transaction.getInitiatedAt(),
                transaction.getSettledAt(),
                ledgerResponses
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Map frontend sort field names to entity field names.
     */
    private String mapSortField(String frontendField) {
        return switch (frontendField) {
            case "createdAt" -> "initiatedAt";
            case "settlementDueDate" -> "settlementDueDate";
            case "amount" -> "amount";
            case "type" -> "type";
            case "status" -> "status";
            case "id" -> "id";
            default -> "initiatedAt";
        };
    }

    /**
     * Convert Transaction to TransactionResponse with full null-safety.
     */
    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
        // Handle all null fields safely
        String senderEmail = transaction.getSenderWallet() != null
            ? transaction.getSenderWallet().getUser().getEmail()
            : "System";

        String receiverEmail = transaction.getReceiverWallet() != null
            ? transaction.getReceiverWallet().getUser().getEmail()
            : "Unknown";

        String settlementDueDate = transaction.getSettlementDueDate() != null
            ? transaction.getSettlementDueDate().toString()
            : null;

        return new TransactionResponse(
                transaction.getId(),
                transaction.getType().name(),
                transaction.getStatus().name(),
                transaction.getAmount(),
                senderEmail,
                receiverEmail,
                transaction.getRemark(),
                settlementDueDate,
                transaction.getInitiatedAt(),
                transaction.getSettledAt()
        );
    }
}