package com.p2ppayment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "simulated_account_transactions")
public class SimulatedAccountTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "simulated_account_id", nullable = false)
    private SimulatedBankAccount simulatedAccount;

    @Column(nullable = false, length = 30)
    private String operationType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 6)
    private String direction;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceBefore;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "related_transaction_id")
    private Long relatedTransactionId;

    @Column(nullable = false, length = 10)
    private String outcome;

    @Column(length = 255)
    private String notes;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (outcome == null) {
            outcome = "SUCCESS";
        }
    }
}