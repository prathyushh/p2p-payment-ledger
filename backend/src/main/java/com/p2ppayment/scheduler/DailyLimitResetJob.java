package com.p2ppayment.scheduler;

import com.p2ppayment.entity.ScheduledJobLog;
import com.p2ppayment.repository.ScheduledJobLogRepository;
import com.p2ppayment.repository.WalletRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class DailyLimitResetJob {

    private final WalletRepository walletRepository;
    private final ScheduledJobLogRepository jobLogRepository;

    public DailyLimitResetJob(WalletRepository walletRepository,
                             ScheduledJobLogRepository jobLogRepository) {
        this.walletRepository = walletRepository;
        this.jobLogRepository = jobLogRepository;
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void resetDailyLimits() {
        ScheduledJobLog jobLog = ScheduledJobLog.builder()
                .jobName("DAILY_LIMIT_RESET")
                .startedAt(LocalDateTime.now())
                .status(ScheduledJobLog.Status.RUNNING)
                .build();
        jobLog = jobLogRepository.save(jobLog);

        try {
            // Reset daily_spent for all wallets atomically
            int updated = walletRepository.resetDailySpent();
            
            jobLog.setEndedAt(LocalDateTime.now());
            jobLog.setStatus(ScheduledJobLog.Status.SUCCESS);
            jobLog.setRecordsProcessed(updated);
            jobLogRepository.save(jobLog);
            
        } catch (Exception e) {
            jobLog.setEndedAt(LocalDateTime.now());
            jobLog.setStatus(ScheduledJobLog.Status.FAILED);
            jobLog.setErrorMessage(e.getMessage());
            jobLogRepository.save(jobLog);
        }
    }
}
