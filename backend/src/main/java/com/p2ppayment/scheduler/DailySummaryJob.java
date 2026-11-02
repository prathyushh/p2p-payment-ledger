package com.p2ppayment.scheduler;

import com.p2ppayment.entity.DailySummary;
import com.p2ppayment.entity.Notification;
import com.p2ppayment.entity.ScheduledJobLog;
import com.p2ppayment.repository.DailySummaryRepository;
import com.p2ppayment.repository.FraudAlertRepository;
import com.p2ppayment.repository.LedgerEntryRepository;
import com.p2ppayment.repository.ScheduledJobLogRepository;
import com.p2ppayment.repository.TransactionRepository;
import com.p2ppayment.repository.UserRepository;
import com.p2ppayment.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DailySummaryJob {

    private static final Logger logger = LoggerFactory.getLogger(DailySummaryJob.class);

    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final UserRepository userRepository;
    private final DailySummaryRepository dailySummaryRepository;
    private final ScheduledJobLogRepository jobLogRepository;
    private final NotificationService notificationService;

    public DailySummaryJob(TransactionRepository transactionRepository,
                           LedgerEntryRepository ledgerEntryRepository,
                           FraudAlertRepository fraudAlertRepository,
                           UserRepository userRepository,
                           DailySummaryRepository dailySummaryRepository,
                           ScheduledJobLogRepository jobLogRepository,
                           NotificationService notificationService) {
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.userRepository = userRepository;
        this.dailySummaryRepository = dailySummaryRepository;
        this.jobLogRepository = jobLogRepository;
        this.notificationService = notificationService;
    }

    /**
     * Runs at 11 PM every day.
     * Aggregates today''s activity and performs reconciliation on settled transactions only.
     */
    @Scheduled(cron = "0 0 23 * * *")
    @Transactional
    public void runDailySummary() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        logger.info("[DAILY-SUMMARY] Job started at {}", now);

        ScheduledJobLog jobLog = ScheduledJobLog.builder()
                .jobName("DAILY_SUMMARY")
                .startedAt(now)
                .status(ScheduledJobLog.Status.RUNNING)
                .build();
        jobLog = jobLogRepository.save(jobLog);

        try {
            // 1. Transfers initiated today
            Long totalInitiated = transactionRepository.countInitiatedTransactionsForDate(today);
            BigDecimal totalInitiatedAmount = transactionRepository.sumInitiatedAmountForDate(today);
            logger.info("[DAILY-SUMMARY] Transfers initiated today: {} = Rs{}", totalInitiated, totalInitiatedAmount);

            // 2. Transfers settled today
            Long totalSettled = transactionRepository.countSettledTransactionsForDate(today);
            BigDecimal totalSettledAmount = transactionRepository.sumSettledAmountForDate(today);
            logger.info("[DAILY-SUMMARY] Transfers settled today: {} = Rs{}", totalSettled, totalSettledAmount);

            // 3. Fraud alerts raised today
            Long fraudAlertsCount = fraudAlertRepository.countByAlertDateForToday(today);
            logger.info("[DAILY-SUMMARY] Fraud alerts today: {}", fraudAlertsCount);

            // 4. New user registrations today
            Long newUsers = userRepository.countNewRegistrationsForDate(today);
            logger.info("[DAILY-SUMMARY] New registrations today: {}", newUsers);

            // 5. Total pending settlement amount (ALL pending, not just today)
            BigDecimal totalPendingAmount = transactionRepository.sumPendingSettlementAmount();
            logger.info("[DAILY-SUMMARY] Total pending settlement (all time): Rs{}", totalPendingAmount);

            // 6. Reconciliation — SETTLED transactions only
            // For each settled P2P transfer: DEBIT (sender) and CREDIT (receiver) must be created
            // So total debits == total credits for the day
            BigDecimal debitSum = ledgerEntryRepository.sumDebitForDate(today);
            BigDecimal creditSum = ledgerEntryRepository.sumCreditForDate(today);
            boolean mismatch = debitSum.compareTo(creditSum) != 0;

            logger.info("[DAILY-SUMMARY] Reconciliation - DEBIT sum: Rs{}, CREDIT sum: Rs{}, Mismatch: {}",
                    debitSum, creditSum, mismatch);

            // 7. Save daily summary record
            // Skip if already exists for today (idempotent)
            if (dailySummaryRepository.findBySummaryDate(today).isEmpty()) {
                DailySummary summary = DailySummary.builder()
                        .summaryDate(today)
                        .totalTransfersInitiated(totalInitiated.intValue())
                        .totalTransfersInitiatedAmount(totalInitiatedAmount)
                        .totalTransfersSettled(totalSettled.intValue())
                        .totalTransfersSettledAmount(totalSettledAmount)
                        .fraudAlertsRaised(fraudAlertsCount.intValue())
                        .newUserRegistrations(newUsers.intValue())
                        .totalPendingSettlementAmount(totalPendingAmount)
                        .reconciliationDebitSum(debitSum)
                        .reconciliationCreditSum(creditSum)
                        .reconciliationMismatch(mismatch)
                        .build();
                dailySummaryRepository.save(summary);
                logger.info("[DAILY-SUMMARY] Summary saved for {}", today);
            } else {
                logger.warn("[DAILY-SUMMARY] Summary for {} already exists, skipping save", today);
            }

            // 8. If mismatch, alert all admins
            if (mismatch) {
                logger.warn("[DAILY-SUMMARY] *** RECONCILIATION MISMATCH *** DEBIT={} CREDIT={}", debitSum, creditSum);
                userRepository.findAllAdminUsers().forEach(admin -> {
                    try {
                        notificationService.create(
                                admin.getId(),
                                Notification.Type.FRAUD_ALERT_HIGH_VALUE,
                                "CRITICAL: Reconciliation Mismatch",
                                "Daily reconciliation for " + today +
                                " detected a mismatch. DEBIT sum: Rs" + debitSum +
                                ", CREDIT sum: Rs" + creditSum +
                                ". Difference: Rs" + debitSum.subtract(creditSum).abs() +
                                ". Please investigate immediately."
                        );
                    } catch (Exception e) {
                        logger.error("[DAILY-SUMMARY] Failed to notify admin {}: {}", admin.getEmail(), e.getMessage());
                    }
                });
            }

            // 9. Finalize job log
            jobLog.setEndedAt(LocalDateTime.now());
            jobLog.setStatus(ScheduledJobLog.Status.SUCCESS);
            jobLog.setRecordsProcessed(1);
            if (mismatch) {
                jobLog.setErrorMessage("Reconciliation mismatch: DEBIT=" + debitSum + " CREDIT=" + creditSum);
            }
            jobLogRepository.save(jobLog);

            logger.info("[DAILY-SUMMARY] Job completed successfully for {}", today);

        } catch (Exception e) {
            logger.error("[DAILY-SUMMARY] Job FAILED", e);
            jobLog.setEndedAt(LocalDateTime.now());
            jobLog.setStatus(ScheduledJobLog.Status.FAILED);
            jobLog.setErrorMessage(e.getMessage());
            jobLogRepository.save(jobLog);
        }
    }
}
