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
@Table(name = "scheduled_job_log")
public class ScheduledJobLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String jobName;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = true)
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = true)
    private Integer recordsProcessed;

    @Column(nullable = true)
    private Integer recordsFailed;

    @Column(nullable = true, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = true)
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = Status.RUNNING;
        }
    }

    public enum Status {
        RUNNING, SUCCESS, FAILED
    }
}