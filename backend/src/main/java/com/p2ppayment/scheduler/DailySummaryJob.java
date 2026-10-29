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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DailySummaryJob {

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

    @Scheduled(cron = "0 0 23 * * *")
    @Transactional
    public void runDailySummary() {
        ScheduledJobLog jobLog = ScheduledJobLog.builder()
                .jobName("DAILY_SUMMARY")
                .startedAt(LocalDateTime.now())
                .status(ScheduledJobLog.Status.RUNNING)
                .build();
        jobLog = jobLogRepository.save(jobLog);

        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);

            // Aggregate daily statistics
            Long totalInitiated = transactionRepository.countInitiatedTransactionsForDate(yesterday);
            BigDecimal totalInitiatedAmount = transactionRepository.sumInitiatedAmountForDate(yesterday);

            Long totalSettled = transactionRepository.countSettledTransactionsForDate(yesterday);
            BigDecimal totalSettledAmount = transactionRepository.sumSettledAmountForDate(yesterday);

            Long fraudAlertsCount = fraudAlertRepository.countByAlertDateForToday(yesterday);

            Long pendingCount = transactionRepository.countPendingTransactionsForDate(yesterday);
            BigDecimal totalPendingAmount = transactionRepository.sumPendingAmountForDate(yesterday);

            // Reconciliation: sum ledger entries
            BigDecimal debitSum = ledgerEntryRepository.sumDebitForDate(yesterday);
            BigDecimal creditSum = ledgerEntryRepository.sumCreditForDate(yesterday);

            // Flag mismatch if debit and credit sums don't match (they should be equal in normal operations)
            Boolean mismatch = !debitSum.equals(creditSum);

            // Create DailySummary record
            DailySummary summary = DailySummary.builder()
                    .summaryDate(yesterday)
                    .totalTransfersInitiated(totalInitiated.intValue())
                    .totalTransfersInitiatedAmount(totalInitiatedAmount)
                    .totalTransfersSettled(totalSettled.intValue())
                    .totalTransfersSettledAmount(totalSettledAmount)
                    .fraudAlertsRaised(fraudAlertsCount.intValue())
                    .newUserRegistrations(0)  // TODO: can be calculated from users.created_at
                    .totalPendingSettlementAmount(totalPendingAmount)
                    .reconciliationDebitSum(debitSum)
                    .reconciliationCreditSum(creditSum)
                    .reconciliationMismatch(mismatch)
                    .build();

            dailySummaryRepository.save(summary);

            jobLog.setEndedAt(LocalDateTime.now());
            jobLog.setStatus(ScheduledJobLog.Status.SUCCESS);
            jobLog.setRecordsProcessed(1);
            if (mismatch) {
                jobLog.setErrorMessage("Reconciliation mismatch detected: DEBIT=" + debitSum + ", CREDIT=" + creditSum);
            }
            jobLogRepository.save(jobLog);

            // Send notification to admin if mismatch detected
            if (mismatch) {
                try {
                    // Find admin users (role = ADMIN)
                    userRepository.findAllAdminUsers().forEach(admin -> {
                        try {
                            notificationService.create(
                                admin.getId(),
                                Notification.Type.FRAUD_ALERT_HIGH_VALUE,
                                "Reconciliation Mismatch Detected",
                                "Daily reconciliation for " + yesterday + " detected a mismatch: DEBIT=" + debitSum + ", CREDIT=" + creditSum + ". Please investigate."
                            );
                        } catch (Exception e) {
                            // Log but do not fail the job
                        }
                    });
                } catch (Exception e) {
                    // Log but do not fail the job if notification creation fails
                }
            }
            
        } catch (Exception e) {
            jobLog.setEndedAt(LocalDateTime.now());
            jobLog.setStatus(ScheduledJobLog.Status.FAILED);
            jobLog.setErrorMessage(e.getMessage());
            jobLogRepository.save(jobLog);
        }
    }
}
