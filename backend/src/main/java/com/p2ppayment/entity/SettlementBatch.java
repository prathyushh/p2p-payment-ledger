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
@Table(name = "settlement_batches")
public class SettlementBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_log_id", nullable = true)
    private ScheduledJobLog jobLog;

    @Column(nullable = false)
    private LocalDate batchDate;

    @Column(nullable = false)
    private Integer transactionsSettled;

    @Column(nullable = false)
    private Integer transactionsFailed;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmountSettled;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (batchDate == null) {
            batchDate = LocalDate.now();
        }
        if (transactionsSettled == null) {
            transactionsSettled = 0;
        }
        if (transactionsFailed == null) {
            transactionsFailed = 0;
        }
        if (totalAmountSettled == null) {
            totalAmountSettled = BigDecimal.ZERO;
        }
    }
}