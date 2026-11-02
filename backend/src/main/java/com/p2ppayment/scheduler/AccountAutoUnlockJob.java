package com.p2ppayment.scheduler;

import com.p2ppayment.entity.Notification;
import com.p2ppayment.entity.ScheduledJobLog;
import com.p2ppayment.entity.User;
import com.p2ppayment.repository.ScheduledJobLogRepository;
import com.p2ppayment.repository.UserRepository;
import com.p2ppayment.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class AccountAutoUnlockJob {

    private static final Logger logger = LoggerFactory.getLogger(AccountAutoUnlockJob.class);

    private final UserRepository userRepository;
    private final ScheduledJobLogRepository jobLogRepository;
    private final NotificationService notificationService;

    public AccountAutoUnlockJob(UserRepository userRepository,
                                ScheduledJobLogRepository jobLogRepository,
                                NotificationService notificationService) {
        this.userRepository = userRepository;
        this.jobLogRepository = jobLogRepository;
        this.notificationService = notificationService;
    }

    /**
     * Runs every 5 minutes.
     * Finds all accounts whose lock has expired (locked_until <= NOW)
     * and unlocks them, resetting the failed attempt counter.
     * Sends an "account unlocked" notification to each user.
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void unlockExpiredLocks() {
        LocalDateTime now = LocalDateTime.now();
        logger.info("[AUTO-UNLOCK] Job started at {}", now);

        ScheduledJobLog jobLog = ScheduledJobLog.builder()
                .jobName("ACCOUNT_AUTO_UNLOCK")
                .startedAt(now)
                .status(ScheduledJobLog.Status.RUNNING)
                .build();
        jobLog = jobLogRepository.save(jobLog);

        int unlocked = 0;

        try {
            // Find all users whose lock period has expired
            List<User> expiredLocks = userRepository.findLockedAccountsToUnlock(now);

            logger.info("[AUTO-UNLOCK] Found {} account(s) to unlock", expiredLocks.size());

            for (User user : expiredLocks) {
                logger.info("[AUTO-UNLOCK] Unlocking user: {} (locked until: {})",
                        user.getEmail(), user.getLockedUntil());

                // Use direct SQL to unlock + reset counter (same as LoginAttemptService)
                userRepository.unlockAccount(user.getId());
                unlocked++;

                // Send notification
                try {
                    notificationService.create(
                            user.getId(),
                            Notification.Type.ACCOUNT_UNLOCKED,
                            "Account Unlocked",
                            "Your account has been automatically unlocked. You can now log in again."
                    );
                    logger.info("[AUTO-UNLOCK] Notification sent to: {}", user.getEmail());
                } catch (Exception e) {
                    logger.error("[AUTO-UNLOCK] Failed to send notification to: {}", user.getEmail(), e);
                }
            }

            jobLog.setEndedAt(LocalDateTime.now());
            jobLog.setStatus(ScheduledJobLog.Status.SUCCESS);
            jobLog.setRecordsProcessed(unlocked);
            jobLogRepository.save(jobLog);

            logger.info("[AUTO-UNLOCK] Job completed. Unlocked {} account(s).", unlocked);

        } catch (Exception e) {
            logger.error("[AUTO-UNLOCK] Job failed", e);
            jobLog.setEndedAt(LocalDateTime.now());
            jobLog.setStatus(ScheduledJobLog.Status.FAILED);
            jobLog.setErrorMessage(e.getMessage());
            jobLogRepository.save(jobLog);
        }
    }
}
