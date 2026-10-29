package com.p2ppayment.scheduler;

import com.p2ppayment.entity.Notification;
import com.p2ppayment.entity.ScheduledJobLog;
import com.p2ppayment.entity.User;
import com.p2ppayment.repository.ScheduledJobLogRepository;
import com.p2ppayment.repository.UserRepository;
import com.p2ppayment.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class AccountAutoUnlockJob {

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

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void unlockExpiredLocks() {
        ScheduledJobLog jobLog = ScheduledJobLog.builder()
                .jobName("ACCOUNT_AUTO_UNLOCK")
                .startedAt(LocalDateTime.now())
                .status(ScheduledJobLog.Status.RUNNING)
                .build();
        jobLog = jobLogRepository.save(jobLog);

        int unlocked = 0;

        try {
            LocalDateTime now = LocalDateTime.now();
            List<User> lockedUsers = userRepository.findLockedAccountsToUnlock(now);

            for (User user : lockedUsers) {
                user.setAccountLocked(false);
                user.setLockedUntil(null);
                userRepository.save(user);
                unlocked++;
                
                // Create notification for account unlock
                try {
                    notificationService.create(
                        user.getId(),
                        Notification.Type.ACCOUNT_UNLOCKED,
                        "Account Unlocked",
                        "Your account has been automatically unlocked after the lock period expired."
                    );
                } catch (Exception e) {
                    // Log but do not fail the unlock job if notification creation fails
                }
            }

            jobLog.setEndedAt(LocalDateTime.now());
            jobLog.setStatus(ScheduledJobLog.Status.SUCCESS);
            jobLog.setRecordsProcessed(unlocked);
            jobLogRepository.save(jobLog);
            
        } catch (Exception e) {
            jobLog.setEndedAt(LocalDateTime.now());
            jobLog.setStatus(ScheduledJobLog.Status.FAILED);
            jobLog.setErrorMessage(e.getMessage());
            jobLogRepository.save(jobLog);
        }
    }
}
