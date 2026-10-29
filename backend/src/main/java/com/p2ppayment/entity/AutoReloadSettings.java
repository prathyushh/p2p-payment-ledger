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
@Table(name = "auto_reload_settings")
public class AutoReloadSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "wallet_id", nullable = false, unique = true)
    private Wallet wallet;

    @Column(nullable = false)
    private Boolean isEnabled;

    @ManyToOne
    @JoinColumn(name = "simulated_account_id", nullable = true)
    private SimulatedBankAccount simulatedAccount;

    @Column(nullable = true, precision = 15, scale = 2)
    private BigDecimal thresholdAmount;

    @Column(nullable = true, precision = 15, scale = 2)
    private BigDecimal reloadAmount;

    @Column(nullable = false)
    private Boolean reloadInProgress;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (isEnabled == null) {
            isEnabled = false;
        }
        if (reloadInProgress == null) {
            reloadInProgress = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}