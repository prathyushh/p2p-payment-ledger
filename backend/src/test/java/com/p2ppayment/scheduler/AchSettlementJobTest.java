
package com.p2ppayment.scheduler;

import com.p2ppayment.entity.ScheduledJobLog;
import com.p2ppayment.entity.SettlementBatch;
import com.p2ppayment.entity.Transaction;
import com.p2ppayment.repository.ScheduledJobLogRepository;
import com.p2ppayment.repository.SettlementBatchRepository;
import com.p2ppayment.repository.TransactionRepository;
import com.p2ppayment.service.SettlementService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AchSettlementJobTest {

    @Mock
    private SettlementService settlementService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ScheduledJobLogRepository jobLogRepository;

    @Mock
    private SettlementBatchRepository settlementBatchRepository;

    @InjectMocks
    private AchSettlementJob job;

    private Transaction transaction(Long id, String amount) {
        Transaction txn = new Transaction();
        txn.setId(id);
        txn.setAmount(new BigDecimal(amount));
        return txn;
    }

    private void stubJobLogSave() {
        when(jobLogRepository.save(any(ScheduledJobLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubSettlementBatchSave() {
        when(settlementBatchRepository.save(any(SettlementBatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void runSettlement_NoPendingTransactions() throws Exception {
        stubJobLogSave();
        stubSettlementBatchSave();

        when(settlementService.getPendingTransactionsDue())
                .thenReturn(List.of());

        job.runSettlement();

        verify(settlementService, never()).settleTransaction(any());
        verify(settlementService, never()).markFailedAndRefund(any());
        verify(settlementBatchRepository).save(any(SettlementBatch.class));
        verify(jobLogRepository, times(2)).save(any(ScheduledJobLog.class));
    }

    @Test
    void runSettlement_AllTransactionsSucceed() throws Exception {
        stubJobLogSave();
        stubSettlementBatchSave();

        Transaction first = transaction(1L, "100.00");
        Transaction second = transaction(2L, "200.00");

        when(settlementService.getPendingTransactionsDue())
                .thenReturn(List.of(first, second));

        job.runSettlement();

        verify(settlementService).settleTransaction(first);
        verify(settlementService).settleTransaction(second);
        verify(settlementService, never()).markFailedAndRefund(any());
        verify(settlementBatchRepository).save(any(SettlementBatch.class));
        verify(jobLogRepository, times(2)).save(any(ScheduledJobLog.class));
    }

    @Test
    void runSettlement_FailedSettlementAttemptsRefund() throws Exception {
        stubJobLogSave();
        stubSettlementBatchSave();

        Transaction txn = transaction(1L, "100.00");

        when(settlementService.getPendingTransactionsDue())
                .thenReturn(List.of(txn));

        doThrow(new RuntimeException("Settlement failed"))
                .when(settlementService).settleTransaction(txn);

        job.runSettlement();

        verify(settlementService).settleTransaction(txn);
        verify(settlementService).markFailedAndRefund(txn);
        verify(settlementBatchRepository).save(any(SettlementBatch.class));
        verify(jobLogRepository, times(2)).save(any(ScheduledJobLog.class));
    }

    @Test
    void runSettlement_RefundFailure_DoesNotStopJob() throws Exception {
        stubJobLogSave();
        stubSettlementBatchSave();

        Transaction failedTxn = transaction(1L, "100.00");
        Transaction successfulTxn = transaction(2L, "250.00");

        when(settlementService.getPendingTransactionsDue())
                .thenReturn(List.of(failedTxn, successfulTxn));

        doThrow(new RuntimeException("Settlement failed"))
                .when(settlementService).settleTransaction(failedTxn);

        doThrow(new RuntimeException("Refund failed"))
                .when(settlementService).markFailedAndRefund(failedTxn);

        job.runSettlement();

        verify(settlementService).settleTransaction(failedTxn);
        verify(settlementService).markFailedAndRefund(failedTxn);
        verify(settlementService).settleTransaction(successfulTxn);
        verify(settlementBatchRepository).save(any(SettlementBatch.class));
        verify(jobLogRepository, times(2)).save(any(ScheduledJobLog.class));
    }

    @Test
    void runSettlement_UnexpectedRetrievalFailure_RecordsFailedJob() throws Exception {
        stubJobLogSave();

        when(settlementService.getPendingTransactionsDue())
                .thenThrow(new RuntimeException("Database unavailable"));

        job.runSettlement();

        verify(settlementService, never()).settleTransaction(any());
        verify(settlementService, never()).markFailedAndRefund(any());
        verify(settlementBatchRepository, never()).save(any(SettlementBatch.class));
        verify(jobLogRepository, times(2)).save(any(ScheduledJobLog.class));
    }
}