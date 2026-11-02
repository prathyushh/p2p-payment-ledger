package com.p2ppayment.scheduler;

import com.p2ppayment.entity.ScheduledJobLog;
import com.p2ppayment.repository.ScheduledJobLogRepository;
import com.p2ppayment.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class DailyLimitResetJob {

    private static final Logger logger = LoggerFactory.getLogger(DailyLimitResetJob.class);

    private final WalletRepository walletRepository;
    private final ScheduledJobLogRepository jobLogRepository;

    public DailyLimitResetJob(WalletRepository walletRepository,
                              ScheduledJobLogRepository jobLogRepository) {
        this.walletRepository = walletRepository;
        this.jobLogRepository = jobLogRepository;
    }

    /**
     * Runs at midnight every day (00:00:00).
     * Resets daily_spent = 0 for ALL wallets so users can transact again.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void resetDailyLimits() {
        LocalDateTime now = LocalDateTime.now();
        logger.info("[DAILY-RESET] Job started at {}", now);

        ScheduledJobLog jobLog = ScheduledJobLog.builder()
                .jobName("DAILY_LIMIT_RESET")
                .startedAt(now)
                .status(ScheduledJobLog.Status.RUNNING)
                .build();
        jobLog = jobLogRepository.save(jobLog);

        try {
            int updated = walletRepository.resetDailySpent();

            jobLog.setEndedAt(LocalDateTime.now());
            jobLog.setStatus(ScheduledJobLog.Status.SUCCESS);
            jobLog.setRecordsProcessed(updated);
            jobLogRepository.save(jobLog);

            logger.info("[DAILY-RESET] Job completed successfully. Reset daily_spent=0 for {} wallet(s).", updated);

        } catch (Exception e) {
            logger.error("[DAILY-RESET] Job FAILED", e);
            jobLog.setEndedAt(LocalDateTime.now());
            jobLog.setStatus(ScheduledJobLog.Status.FAILED);
            jobLog.setErrorMessage(e.getMessage());
            jobLogRepository.save(jobLog);
        }
    }
}
