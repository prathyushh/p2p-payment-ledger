
package com.p2ppayment.scheduler;

import com.p2ppayment.entity.Notification;
import com.p2ppayment.entity.ScheduledJobLog;
import com.p2ppayment.entity.User;
import com.p2ppayment.repository.ScheduledJobLogRepository;
import com.p2ppayment.repository.UserRepository;
import com.p2ppayment.service.NotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountAutoUnlockJobTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ScheduledJobLogRepository jobLogRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AccountAutoUnlockJob job;

    @BeforeEach
    void setUp() {
        when(jobLogRepository.save(any(ScheduledJobLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void unlockExpiredLocks_UnlocksAccountsAndNotifiesUsers() {
        User first = new User();
        first.setId(1L);
        first.setEmail("first@example.com");
        first.setLockedUntil(LocalDateTime.now().minusMinutes(10));

        User second = new User();
        second.setId(2L);
        second.setEmail("second@example.com");
        second.setLockedUntil(LocalDateTime.now().minusMinutes(5));

        when(userRepository.findLockedAccountsToUnlock(any(LocalDateTime.class)))
                .thenReturn(List.of(first, second));

        job.unlockExpiredLocks();

        verify(userRepository).unlockAccount(1L);
        verify(userRepository).unlockAccount(2L);

        verify(notificationService).create(
                eq(1L),
                eq(Notification.Type.ACCOUNT_UNLOCKED),
                eq("Account Unlocked"),
                contains("automatically unlocked")
        );

        verify(notificationService).create(
                eq(2L),
                eq(Notification.Type.ACCOUNT_UNLOCKED),
                eq("Account Unlocked"),
                contains("automatically unlocked")
        );

        verify(jobLogRepository, times(2)).save(any(ScheduledJobLog.class));
    }

    @Test
    void unlockExpiredLocks_NoExpiredAccounts_CompletesSuccessfully() {
        when(userRepository.findLockedAccountsToUnlock(any(LocalDateTime.class)))
                .thenReturn(List.of());

        job.unlockExpiredLocks();

        verify(userRepository, never()).unlockAccount(anyLong());
        verifyNoInteractions(notificationService);
        verify(jobLogRepository, times(2)).save(any(ScheduledJobLog.class));
    }

    @Test
    void unlockExpiredLocks_NotificationFailure_ContinuesProcessing() {
        User first = new User();
        first.setId(1L);
        first.setEmail("first@example.com");

        User second = new User();
        second.setId(2L);
        second.setEmail("second@example.com");

        when(userRepository.findLockedAccountsToUnlock(any(LocalDateTime.class)))
                .thenReturn(List.of(first, second));

        doThrow(new RuntimeException("Notification service unavailable"))
                .when(notificationService)
                .create(
                        eq(1L),
                        eq(Notification.Type.ACCOUNT_UNLOCKED),
                        anyString(),
                        anyString()
                );

        job.unlockExpiredLocks();

        verify(userRepository).unlockAccount(1L);
        verify(userRepository).unlockAccount(2L);

        verify(notificationService, times(2)).create(
                anyLong(),
                eq(Notification.Type.ACCOUNT_UNLOCKED),
                anyString(),
                anyString()
        );
    }

    @Test
    void unlockExpiredLocks_RepositoryFailure_RecordsFailedJob() {
        when(userRepository.findLockedAccountsToUnlock(any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Database unavailable"));

        job.unlockExpiredLocks();

        verify(jobLogRepository, times(2)).save(any(ScheduledJobLog.class));
        verify(userRepository, never()).unlockAccount(anyLong());
    }

    @Test
    void unlockExpiredLocks_UnlockFailure_RecordsFailedJob() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");

        when(userRepository.findLockedAccountsToUnlock(any(LocalDateTime.class)))
                .thenReturn(List.of(user));

        doThrow(new RuntimeException("Unlock failed"))
                .when(userRepository).unlockAccount(1L);

        job.unlockExpiredLocks();

        verify(jobLogRepository, times(2)).save(any(ScheduledJobLog.class));
        verifyNoInteractions(notificationService);
    }
}