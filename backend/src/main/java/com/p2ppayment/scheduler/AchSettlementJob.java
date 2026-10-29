package com.p2ppayment.scheduler;

import com.p2ppayment.entity.ScheduledJobLog;
import com.p2ppayment.entity.SettlementBatch;
import com.p2ppayment.entity.Transaction;
import com.p2ppayment.repository.ScheduledJobLogRepository;
import com.p2ppayment.repository.SettlementBatchRepository;
import com.p2ppayment.repository.TransactionRepository;
import com.p2ppayment.service.SettlementService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class AchSettlementJob {

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

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void runSettlement() {
        ScheduledJobLog jobLog = ScheduledJobLog.builder()
                .jobName("ACH_SETTLEMENT")
                .startedAt(LocalDateTime.now())
                .status(ScheduledJobLog.Status.RUNNING)
                .build();
        jobLog = jobLogRepository.save(jobLog);

        int settled = 0;
        int failed = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        try {
            List<Transaction> dueTransactions = settlementService.getPendingTransactionsDue();

            for (Transaction txn : dueTransactions) {
                try {
                    settlementService.settleTransaction(txn);
                    settled++;
                    totalAmount = totalAmount.add(txn.getAmount());
                } catch (Exception e) {
                    failed++;
                    try {
                        txn.setStatus(Transaction.Status.FAILED);
                        txn.setSettledAt(LocalDateTime.now());
                        transactionRepository.save(txn);
                    } catch (Exception saveError) {
                    }
                }
            }

            jobLog.setEndedAt(LocalDateTime.now());
            jobLog.setStatus(ScheduledJobLog.Status.SUCCESS);
            jobLog.setRecordsProcessed(settled + failed);
            jobLog.setRecordsFailed(failed);
            jobLog.setTotalAmount(totalAmount);
            jobLogRepository.save(jobLog);

            SettlementBatch batch = SettlementBatch.builder()
                    .jobLog(jobLog)
                    .batchDate(LocalDate.now())
                    .transactionsSettled(settled)
                    .transactionsFailed(failed)
                    .totalAmountSettled(totalAmount)
                    .build();
            settlementBatchRepository.save(batch);

        } catch (Exception e) {
            jobLog.setEndedAt(LocalDateTime.now());
            jobLog.setStatus(ScheduledJobLog.Status.FAILED);
            jobLog.setErrorMessage(e.getMessage());
            jobLogRepository.save(jobLog);
        }
    }
}