package com.p2ppayment.entity;

import jakarta.persistence.*;
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
@Entity
@Table(name = "daily_summary")
public class DailySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate summaryDate;

    @Column(nullable = false)
    private Integer totalTransfersInitiated;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalTransfersInitiatedAmount;

    @Column(nullable = false)
    private Integer totalTransfersSettled;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalTransfersSettledAmount;

    @Column(nullable = false)
    private Integer fraudAlertsRaised;

    @Column(nullable = false)
    private Integer newUserRegistrations;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPendingSettlementAmount;

    @Column(nullable = true, precision = 15, scale = 2)
    private BigDecimal reconciliationDebitSum;

    @Column(nullable = true, precision = 15, scale = 2)
    private BigDecimal reconciliationCreditSum;

    @Column(nullable = true)
    private Boolean reconciliationMismatch;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (totalTransfersInitiated == null) {
            totalTransfersInitiated = 0;
        }
        if (totalTransfersInitiatedAmount == null) {
            totalTransfersInitiatedAmount = BigDecimal.ZERO;
        }
        if (totalTransfersSettled == null) {
            totalTransfersSettled = 0;
        }
        if (totalTransfersSettledAmount == null) {
            totalTransfersSettledAmount = BigDecimal.ZERO;
        }
        if (fraudAlertsRaised == null) {
            fraudAlertsRaised = 0;
        }
        if (newUserRegistrations == null) {
            newUserRegistrations = 0;
        }
        if (totalPendingSettlementAmount == null) {
            totalPendingSettlementAmount = BigDecimal.ZERO;
        }
    }
}
