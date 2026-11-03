
package com.p2ppayment.scheduler;

import com.p2ppayment.entity.ScheduledJobLog;
import com.p2ppayment.repository.ScheduledJobLogRepository;
import com.p2ppayment.repository.WalletRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyLimitResetJobTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private ScheduledJobLogRepository jobLogRepository;

    @InjectMocks
    private DailyLimitResetJob job;

    @BeforeEach
    void setUp() {
        when(jobLogRepository.save(any(ScheduledJobLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void resetDailyLimits_Success() {
        when(walletRepository.resetDailySpent()).thenReturn(10);

        job.resetDailyLimits();

        verify(walletRepository).resetDailySpent();
        verify(jobLogRepository, times(2)).save(any(ScheduledJobLog.class));
    }

    @Test
    void resetDailyLimits_NoWalletsToReset() {
        when(walletRepository.resetDailySpent()).thenReturn(0);

        job.resetDailyLimits();

        verify(walletRepository).resetDailySpent();
        verify(jobLogRepository, times(2)).save(any(ScheduledJobLog.class));
    }

    @Test
    void resetDailyLimits_RepositoryFailure_RecordsFailedJob() {
        when(walletRepository.resetDailySpent())
                .thenThrow(new RuntimeException("Database unavailable"));

        job.resetDailyLimits();

        verify(walletRepository).resetDailySpent();
        verify(jobLogRepository, times(2)).save(any(ScheduledJobLog.class));
    }
}