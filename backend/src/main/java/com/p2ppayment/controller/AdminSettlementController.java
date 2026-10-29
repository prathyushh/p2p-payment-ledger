package com.p2ppayment.controller;

import com.p2ppayment.dto.AdminSettlementResponse;
import com.p2ppayment.dto.SettlementQueueResponse;
import com.p2ppayment.entity.Transaction;
import com.p2ppayment.entity.User;
import com.p2ppayment.repository.TransactionRepository;
import com.p2ppayment.service.AuditService;
import com.p2ppayment.service.SettlementService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/settlement")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettlementController {

    private final TransactionRepository transactionRepository;
    private final SettlementService settlementService;
    private final AuditService auditService;

    public AdminSettlementController(TransactionRepository transactionRepository,
                                   SettlementService settlementService,
                                   AuditService auditService) {
        this.transactionRepository = transactionRepository;
        this.settlementService = settlementService;
        this.auditService = auditService;
    }

    /**
     * GET /api/admin/settlement/queue
     * Returns settlement queue summary: counts and amounts for overdue, due today, due tomorrow, and total pending.
     */
    @GetMapping("/queue")
    public ResponseEntity<SettlementQueueResponse> getSettlementQueue() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        Long overduePendingCount = transactionRepository.countOverduePendingTransactions(today);
        BigDecimal overdueAmount = transactionRepository.sumOverduePendingAmount(today);

        Long dueTodayCount = transactionRepository.countPendingTransactionsDueDate(today);
        BigDecimal dueTodayAmount = transactionRepository.sumPendingAmountDueDate(today);

        Long dueTomorrowCount = transactionRepository.countPendingTransactionsDueDate(tomorrow);
        BigDecimal dueTomorrowAmount = transactionRepository.sumPendingAmountDueDate(tomorrow);

        Long totalPendingCount = transactionRepository.countFuturePendingTransactions(today) + dueTodayCount + overduePendingCount;
        BigDecimal totalPendingAmount = transactionRepository.sumFuturePendingAmount(today)
                .add(dueTodayAmount != null ? dueTodayAmount : BigDecimal.ZERO)
                .add(overdueAmount != null ? overdueAmount : BigDecimal.ZERO);

        SettlementQueueResponse response = SettlementQueueResponse.builder()
                .overduePendingCount(overduePendingCount)
                .overdueAmount(overdueAmount != null ? overdueAmount : BigDecimal.ZERO)
                .dueTodayCount(dueTodayCount)
                .dueTodayAmount(dueTodayAmount != null ? dueTodayAmount : BigDecimal.ZERO)
                .dueTomorrowCount(dueTomorrowCount)
                .dueTomorrowAmount(dueTomorrowAmount != null ? dueTomorrowAmount : BigDecimal.ZERO)
                .totalPendingCount(totalPendingCount)
                .totalPendingAmount(totalPendingAmount)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/settlement/pending
     * List pending transactions grouped by settlement date (overdue, due today, due tomorrow, future).
     * Query params: includeOverdue (default true), includeToday (default true), includeTomorrow (default true), includeFuture (default false)
     */
    @GetMapping("/pending")
    public ResponseEntity<?> listPendingTransactions(
            @RequestParam(defaultValue = "true") boolean includeOverdue,
            @RequestParam(defaultValue = "true") boolean includeToday,
            @RequestParam(defaultValue = "true") boolean includeTomorrow,
            @RequestParam(defaultValue = "false") boolean includeFuture) {

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        List<AdminSettlementResponse> overdue = includeOverdue
                ? transactionRepository.findOverduePendingTransactions(today).stream()
                .map(this::mapToAdminSettlementResponse)
                .collect(Collectors.toList())
                : List.of();

        List<AdminSettlementResponse> dueToday = includeToday
                ? transactionRepository.findPendingTransactionsDueDate(today).stream()
                .map(this::mapToAdminSettlementResponse)
                .collect(Collectors.toList())
                : List.of();

        List<AdminSettlementResponse> dueTomorrow = includeTomorrow
                ? transactionRepository.findPendingTransactionsDueDate(tomorrow).stream()
                .map(this::mapToAdminSettlementResponse)
                .collect(Collectors.toList())
                : List.of();

        List<AdminSettlementResponse> future = includeFuture
                ? transactionRepository.findFuturePendingTransactions(today).stream()
                .map(this::mapToAdminSettlementResponse)
                .collect(Collectors.toList())
                : List.of();

        return ResponseEntity.ok(new Object() {
            public final List<AdminSettlementResponse> overdue_transactions = overdue;
            public final List<AdminSettlementResponse> due_today_transactions = dueToday;
            public final List<AdminSettlementResponse> due_tomorrow_transactions = dueTomorrow;
            public final List<AdminSettlementResponse> future_transactions = future;
            public final int total_count = overdue.size() + dueToday.size() + dueTomorrow.size() + future.size();
        });
    }

    /**
     * POST /api/admin/settlement/{id}/trigger
     * Manually trigger settlement for a specific transaction.
     * Logs the action to audit_log.
     */
    @PostMapping("/{id}/trigger")
    public ResponseEntity<?> triggerSettlement(@PathVariable Long id, HttpServletRequest request) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        // Only allow settlement if status is PENDING
        if (transaction.getStatus() != Transaction.Status.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    "Transaction status must be PENDING. Current status: " + transaction.getStatus());
        }

        try {
            // Get current admin user from JWT
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String adminEmail = authentication.getName();
            
            // Log the manual settlement trigger action
            String details = "Manual settlement triggered by admin for transaction " + id + 
                    " (Amount: " + transaction.getAmount() + ")";
            auditService.logAction("SETTLEMENT_TRIGGER", "TRANSACTION", id, 
                    new User() {{ setEmail(adminEmail); }}, details, request.getRemoteAddr());

            // Settle the transaction
            settlementService.settleTransaction(transaction);

            // Fetch updated transaction
            Transaction updatedTransaction = transactionRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                            "Failed to retrieve updated transaction"));

            AdminSettlementResponse response = mapToAdminSettlementResponse(updatedTransaction);

            return ResponseEntity.ok(response);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Error settling transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Convert Transaction to AdminSettlementResponse.
     */
    private AdminSettlementResponse mapToAdminSettlementResponse(Transaction transaction) {
        LocalDate today = LocalDate.now();
        boolean isOverdue = transaction.getStatus() == Transaction.Status.PENDING 
                && transaction.getSettlementDueDate().isBefore(today);

        return AdminSettlementResponse.builder()
                .id(transaction.getId())
                .type(transaction.getType().name())
                .status(transaction.getStatus().name())
                .amount(transaction.getAmount())
                .senderEmail(transaction.getSenderWallet().getUser().getEmail())
                .receiverEmail(transaction.getReceiverWallet().getUser().getEmail())
                .remark(transaction.getRemark())
                .settlementDueDate(transaction.getSettlementDueDate())
                .isOverdue(isOverdue)
                .initiatedAt(transaction.getInitiatedAt())
                .settledAt(transaction.getSettledAt())
                .build();
    }
}