
package com.p2ppayment.scheduler;

import com.p2ppayment.entity.DailySummary;
import com.p2ppayment.entity.Notification;
import com.p2ppayment.entity.ScheduledJobLog;
import com.p2ppayment.entity.User;
import com.p2ppayment.repository.DailySummaryRepository;
import com.p2ppayment.repository.FraudAlertRepository;
import com.p2ppayment.repository.LedgerEntryRepository;
import com.p2ppayment.repository.ScheduledJobLogRepository;
import com.p2ppayment.repository.TransactionRepository;
import com.p2ppayment.repository.UserRepository;
import com.p2ppayment.service.NotificationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailySummaryJobTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DailySummaryRepository dailySummaryRepository;

    @Mock
    private ScheduledJobLogRepository jobLogRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DailySummaryJob job;

    @Test
    void runDailySummary_SuccessfulReconciliation_SavesSummary() {
        stubJobLogSave();
        stubSummaryData();

        when(ledgerEntryRepository.sumDebitForDate(any(LocalDate.class)))
                .thenReturn(new BigDecimal("300.00"));
        when(ledgerEntryRepository.sumCreditForDate(any(LocalDate.class)))
                .thenReturn(new BigDecimal("300.00"));
        when(dailySummaryRepository.findBySummaryDate(any(LocalDate.class)))
                .thenReturn(Optional.empty());
        stubDailySummarySave();

        job.runDailySummary();

        verify(dailySummaryRepository).save(any(DailySummary.class));
        verify(notificationService, never()).create(
                anyLong(),
                any(Notification.Type.class),
                anyString(),
                anyString()
        );
        verify(jobLogRepository, times(2)).save(any(ScheduledJobLog.class));
    }

    @Test
    void runDailySummary_ReconciliationMismatch_NotifiesAdmins() {
        stubJobLogSave();
        stubSummaryData();

        User admin = new User();
        admin.setId(99L);
        admin.setEmail("admin@example.com");

        when(ledgerEntryRepository.sumDebitForDate(any(LocalDate.class)))
                .thenReturn(new BigDecimal("300.00"));
        when(ledgerEntryRepository.sumCreditForDate(any(LocalDate.class)))
                .thenReturn(new BigDecimal("250.00"));
        when(userRepository.findAllAdminUsers())
                .thenReturn(List.of(admin));
        when(dailySummaryRepository.findBySummaryDate(any(LocalDate.class)))
                .thenReturn(Optional.empty());
        stubDailySummarySave();

        job.runDailySummary();

        verify(notificationService).create(
                eq(99L),
                eq(Notification.Type.FRAUD_ALERT_HIGH_VALUE),
                eq("CRITICAL: Reconciliation Mismatch"),
                contains("detected a mismatch")
        );
        verify(dailySummaryRepository).save(any(DailySummary.class));
    }

    @Test
    void runDailySummary_ExistingSummary_SkipsCreatingAnother() {
        stubJobLogSave();
        stubSummaryData();

        DailySummary existing = DailySummary.builder()
                .summaryDate(LocalDate.now())
                .build();

        when(ledgerEntryRepository.sumDebitForDate(any(LocalDate.class)))
                .thenReturn(new BigDecimal("300.00"));
        when(ledgerEntryRepository.sumCreditForDate(any(LocalDate.class)))
                .thenReturn(new BigDecimal("300.00"));
        when(dailySummaryRepository.findBySummaryDate(any(LocalDate.class)))
                .thenReturn(Optional.of(existing));

        job.runDailySummary();

        verify(dailySummaryRepository, never()).save(any(DailySummary.class));
        verify(jobLogRepository, times(2)).save(any(ScheduledJobLog.class));
    }

    @Test
    void runDailySummary_NotificationFailure_DoesNotFailJob() {
        stubJobLogSave();
        stubSummaryData();

        User admin = new User();
        admin.setId(99L);
        admin.setEmail("admin@example.com");

        when(ledgerEntryRepository.sumDebitForDate(any(LocalDate.class)))
                .thenReturn(new BigDecimal("300.00"));
        when(ledgerEntryRepository.sumCreditForDate(any(LocalDate.class)))
                .thenReturn(new BigDecimal("250.00"));
        when(userRepository.findAllAdminUsers())
                .thenReturn(List.of(admin));
        when(dailySummaryRepository.findBySummaryDate(any(LocalDate.class)))
                .thenReturn(Optional.empty());
        stubDailySummarySave();

        doThrow(new RuntimeException("Notification service unavailable"))
                .when(notificationService)
                .create(
                        eq(99L),
                        eq(Notification.Type.FRAUD_ALERT_HIGH_VALUE),
                        anyString(),
                        anyString()
                );

        job.runDailySummary();

        verify(dailySummaryRepository).save(any(DailySummary.class));
        verify(jobLogRepository, times(2)).save(any(ScheduledJobLog.class));
    }

    @Test
    void runDailySummary_AdminLookupFailure_IsHandledByJob() {
        stubJobLogSave();
        stubSummaryData();

        when(ledgerEntryRepository.sumDebitForDate(any(LocalDate.class)))
                .thenReturn(new BigDecimal("300.00"));
        when(ledgerEntryRepository.sumCreditForDate(any(LocalDate.class)))
                .thenReturn(new BigDecimal("250.00"));
        when(userRepository.findAllAdminUsers())
                .thenThrow(new RuntimeException("Admin lookup failed"));

        job.runDailySummary();

        verify(jobLogRepository, times(2)).save(any(ScheduledJobLog.class));
    }

    @Test
    void runDailySummary_RepositoryFailure_RecordsFailedJob() {
        stubJobLogSave();

        when(transactionRepository.countInitiatedTransactionsForDate(any(LocalDate.class)))
                .thenThrow(new RuntimeException("Database unavailable"));

        job.runDailySummary();

        verify(dailySummaryRepository, never()).save(any(DailySummary.class));
        verify(jobLogRepository, times(2)).save(any(ScheduledJobLog.class));
    }

    private void stubJobLogSave() {
        when(jobLogRepository.save(any(ScheduledJobLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubDailySummarySave() {
        when(dailySummaryRepository.save(any(DailySummary.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubSummaryData() {
        when(transactionRepository.countInitiatedTransactionsForDate(any(LocalDate.class)))
                .thenReturn(5L);
        when(transactionRepository.sumInitiatedAmountForDate(any(LocalDate.class)))
                .thenReturn(new BigDecimal("500.00"));
        when(transactionRepository.countSettledTransactionsForDate(any(LocalDate.class)))
                .thenReturn(3L);
        when(transactionRepository.sumSettledAmountForDate(any(LocalDate.class)))
                .thenReturn(new BigDecimal("300.00"));
        when(fraudAlertRepository.countByAlertDateForToday(any(LocalDate.class)))
                .thenReturn(1L);
        when(userRepository.countNewRegistrationsForDate(any(LocalDate.class)))
                .thenReturn(2L);
        when(transactionRepository.sumPendingSettlementAmount())
                .thenReturn(new BigDecimal("200.00"));
    }
}