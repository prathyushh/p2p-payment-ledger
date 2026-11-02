package com.p2ppayment.scheduler;

import com.p2ppayment.entity.ScheduledJobLog;
import com.p2ppayment.entity.SettlementBatch;
import com.p2ppayment.entity.Transaction;
import com.p2ppayment.repository.ScheduledJobLogRepository;
import com.p2ppayment.repository.SettlementBatchRepository;
import com.p2ppayment.repository.TransactionRepository;
import com.p2ppayment.service.SettlementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class AchSettlementJob {

    private static final Logger logger = LoggerFactory.getLogger(AchSettlementJob.class);

    private final SettlementService settlementService;
    private final TransactionRepository transactionRepository;
    private final ScheduledJobLogRepository jobLogRepository;
    private final SettlementBatchRepository settlementBatchRepository;

    public AchSettlementJob(SettlementService settlementService,
                            TransactionRepository transactionRepository,
                            ScheduledJobLogRepository jobLogRepository,
                            SettlementBatchRepository settlementBatchRepository) {
        this.settlementService = settlementService;
        this.transactionRepository = transactionRepository;
        this.jobLogRepository = jobLogRepository;
        this.settlementBatchRepository = settlementBatchRepository;
    }

    /**
     * Runs every hour.
     * Finds PENDING transactions whose settlement_due_date <= today.
     * Settles each one independently — failure of one does NOT affect others.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void runSettlement() {
        LocalDateTime now = LocalDateTime.now();
        logger.info("[ACH-SETTLEMENT] Job started at {}", now);

        // Save job log (its own transaction)
        ScheduledJobLog jobLog = saveJobLog("ACH_SETTLEMENT", now, ScheduledJobLog.Status.RUNNING);

        int settled = 0;
        int failed = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        try {
            List<Transaction> dueTransactions = settlementService.getPendingTransactionsDue();
            logger.info("[ACH-SETTLEMENT] Found {} pending transaction(s) due for settlement", dueTransactions.size());

            for (Transaction txn : dueTransactions) {
                try {
                    logger.info("[ACH-SETTLEMENT] Settling transaction ID: {} amount: Rs{} sender→receiver",
                            txn.getId(), txn.getAmount());

                    // Each settlement runs in its own transaction in SettlementService
                    settlementService.settleTransaction(txn);
                    settled++;
                    totalAmount = totalAmount.add(txn.getAmount());

                    logger.info("[ACH-SETTLEMENT] Transaction {} settled OK", txn.getId());

                } catch (Exception e) {
                    failed++;
                    logger.error("[ACH-SETTLEMENT] Transaction {} FAILED: {}", txn.getId(), e.getMessage());

                    // Mark transaction as FAILED and refund sender — own transaction
                    try {
                        settlementService.markFailedAndRefund(txn);
                    } catch (Exception refundError) {
                        logger.error("[ACH-SETTLEMENT] Refund also failed for transaction {}: {}",
                                txn.getId(), refundError.getMessage());
                    }
                }
            }

            logger.info("[ACH-SETTLEMENT] Job completed. Settled: {}, Failed: {}, Total: Rs{}",
                    settled, failed, totalAmount);

            // Update job log
            finalizeJobLog(jobLog, ScheduledJobLog.Status.SUCCESS, settled + failed, failed, totalAmount, null);

            // Save settlement batch record
            saveSettlementBatch(jobLog, settled, failed, totalAmount);

        } catch (Exception e) {
            logger.error("[ACH-SETTLEMENT] Job FAILED with unexpected error", e);
            finalizeJobLog(jobLog, ScheduledJobLog.Status.FAILED, 0, 0, BigDecimal.ZERO, e.getMessage());
        }
    }

    @Transactional
    protected ScheduledJobLog saveJobLog(String jobName, LocalDateTime startedAt, ScheduledJobLog.Status status) {
        return jobLogRepository.save(ScheduledJobLog.builder()
                .jobName(jobName)
                .startedAt(startedAt)
                .status(status)
                .build());
    }

    @Transactional
    protected void finalizeJobLog(ScheduledJobLog jobLog, ScheduledJobLog.Status status,
                                   int processed, int failed, BigDecimal totalAmount, String error) {
        jobLog.setEndedAt(LocalDateTime.now());
        jobLog.setStatus(status);
        jobLog.setRecordsProcessed(processed);
        jobLog.setRecordsFailed(failed);
        jobLog.setTotalAmount(totalAmount);
        jobLog.setErrorMessage(error);
        jobLogRepository.save(jobLog);
    }

    @Transactional
    protected void saveSettlementBatch(ScheduledJobLog jobLog, int settled, int failed, BigDecimal totalAmount) {
        settlementBatchRepository.save(SettlementBatch.builder()
                .jobLog(jobLog)
                .batchDate(LocalDate.now())
                .transactionsSettled(settled)
                .transactionsFailed(failed)
                .totalAmountSettled(totalAmount)
                .build());
    }
}
