package com.p2ppayment.controller;

import com.p2ppayment.dto.DailySummaryResponse;
import com.p2ppayment.entity.DailySummary;
import com.p2ppayment.repository.DailySummaryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/reconciliation")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReconciliationController {

    private final DailySummaryRepository dailySummaryRepository;

    public AdminReconciliationController(DailySummaryRepository dailySummaryRepository) {
        this.dailySummaryRepository = dailySummaryRepository;
    }

    /**
     * GET /api/admin/reconciliation
     * Returns today's reconciliation summary (debit/credit totals and mismatch status).
     * If no data for today exists, returns 404.
     */
    @GetMapping
    public ResponseEntity<DailySummaryResponse> getTodayReconciliation() {
        LocalDate today = LocalDate.now();
        Optional<DailySummary> summary = dailySummaryRepository.findBySummaryDate(today);
        
        if (summary.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                    "No reconciliation data found for today (" + today + ")");
        }
        
        DailySummaryResponse response = mapToDailySummaryResponse(summary.get());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/reconciliation/{date}
     * Returns reconciliation summary for a specific date (format: YYYY-MM-DD).
     * If no data for that date exists, returns 404.
     */
    @GetMapping("/{date}")
    public ResponseEntity<DailySummaryResponse> getReconciliationByDate(@PathVariable String date) {
        try {
            LocalDate summaryDate = LocalDate.parse(date);
            Optional<DailySummary> summary = dailySummaryRepository.findBySummaryDate(summaryDate);
            
            if (summary.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "No reconciliation data found for date: " + date);
            }
            
            DailySummaryResponse response = mapToDailySummaryResponse(summary.get());
            return ResponseEntity.ok(response);
        } catch (java.time.format.DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Invalid date format. Use YYYY-MM-DD: " + e.getMessage());
        }
    }

    /**
     * Convert DailySummary entity to response DTO.
     */
    private DailySummaryResponse mapToDailySummaryResponse(DailySummary summary) {
        return DailySummaryResponse.builder()
                .id(summary.getId())
                .summaryDate(summary.getSummaryDate())
                .totalTransfersInitiated(summary.getTotalTransfersInitiated())
                .totalTransfersInitiatedAmount(summary.getTotalTransfersInitiatedAmount())
                .totalTransfersSettled(summary.getTotalTransfersSettled())
                .totalTransfersSettledAmount(summary.getTotalTransfersSettledAmount())
                .fraudAlertsRaised(summary.getFraudAlertsRaised())
                .newUserRegistrations(summary.getNewUserRegistrations())
                .totalPendingSettlementAmount(summary.getTotalPendingSettlementAmount())
                .reconciliationDebitSum(summary.getReconciliationDebitSum())
                .reconciliationCreditSum(summary.getReconciliationCreditSum())
                .reconciliationMismatch(summary.getReconciliationMismatch())
                .createdAt(summary.getCreatedAt())
                .build();
    }
}