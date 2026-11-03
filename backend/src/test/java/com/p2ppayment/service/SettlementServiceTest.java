package com.p2ppayment.service;

import com.p2ppayment.entity.LedgerEntry;
import com.p2ppayment.entity.Notification;
import com.p2ppayment.entity.Transaction;
import com.p2ppayment.entity.User;
import com.p2ppayment.entity.Wallet;
import com.p2ppayment.repository.LedgerEntryRepository;
import com.p2ppayment.repository.TransactionRepository;
import com.p2ppayment.repository.UserRepository;
import com.p2ppayment.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private LedgerEntryRepository ledgerEntryRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks private SettlementService settlementService;

    private Transaction transaction;
    private Wallet senderWallet;
    private Wallet receiverWallet;
    private User sender;
    private User receiver;

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setId(1L);
        sender.setEmail("sender@example.com");

        receiver = new User();
        receiver.setId(2L);
        receiver.setEmail("receiver@example.com");

        senderWallet = new Wallet();
        senderWallet.setId(1L);
        senderWallet.setUser(sender);
        senderWallet.setBalance(money("5000.00"));
        senderWallet.setDailySpent(money("1000.00"));
        senderWallet.setStatus(Wallet.Status.ACTIVE);

        receiverWallet = new Wallet();
        receiverWallet.setId(2L);
        receiverWallet.setUser(receiver);
        receiverWallet.setBalance(money("2000.00"));
        receiverWallet.setDailySpent(BigDecimal.ZERO);
        receiverWallet.setStatus(Wallet.Status.ACTIVE);

        transaction = new Transaction();
        transaction.setId(1L);
        transaction.setSenderWallet(senderWallet);
        transaction.setReceiverWallet(receiverWallet);
        transaction.setAmount(money("1000.00"));
        transaction.setType(Transaction.Type.P2P_TRANSFER);
        transaction.setStatus(Transaction.Status.PENDING);
        transaction.setSettlementDueDate(LocalDate.now());
    }

    private void stubSaves() {
        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(ledgerEntryRepository.save(any(LedgerEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubNotificationSuccess() {
        when(notificationService.create(
                anyLong(), any(Notification.Type.class), anyString(), anyString(), any(Transaction.class)))
                .thenReturn(new Notification());
    }

    @Test
    void getPendingTransactionsDue_returnsTransactions() {
        when(transactionRepository.findDueTransactions(any(LocalDate.class)))
                .thenReturn(List.of(transaction));

        List<Transaction> result = settlementService.getPendingTransactionsDue();

        assertEquals(1, result.size());
        assertSame(transaction, result.get(0));
        verify(transactionRepository).findDueTransactions(any(LocalDate.class));
    }

    @Test
    void getPendingTransactionsDue_noTransactions_returnsEmptyList() {
        when(transactionRepository.findDueTransactions(any(LocalDate.class)))
                .thenReturn(List.of());

        assertTrue(settlementService.getPendingTransactionsDue().isEmpty());
    }

    @Test
    void settleTransaction_success_creditsReceiverAndWritesLedger() throws Exception {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        stubSaves();
        stubNotificationSuccess();

        settlementService.settleTransaction(transaction);

        assertEquals(money("3000.00"), receiverWallet.getBalance());
        assertEquals(Transaction.Status.SUCCESS, transaction.getStatus());
        assertNotNull(transaction.getSettledAt());
        verify(walletRepository).save(receiverWallet);
        verify(transactionRepository).save(transaction);

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(captor.capture());
        LedgerEntry entry = captor.getValue();
        assertSame(receiverWallet, entry.getWallet());
        assertSame(transaction, entry.getTransaction());
        assertEquals(LedgerEntry.Type.CREDIT, entry.getType());
        assertEquals(money("1000.00"), entry.getAmount());
        assertEquals(money("2000.00"), entry.getBalanceBefore());
        assertEquals(money("3000.00"), entry.getBalanceAfter());

        verify(notificationService, times(2)).create(
                anyLong(), eq(Notification.Type.TRANSFER_SETTLED), anyString(), anyString(), same(transaction));
    }

    @Test
    void settleTransaction_alreadySettled_doesNotChangeWallets() throws Exception {
        transaction.setStatus(Transaction.Status.SUCCESS);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        settlementService.settleTransaction(transaction);

        verify(walletRepository, never()).save(any(Wallet.class));
        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
        verifyNoInteractions(notificationService);
    }

    @Test
    void settleTransaction_frozenReceiver_declinesAndRefundsSender() throws Exception {
        receiverWallet.setStatus(Wallet.Status.FROZEN);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        stubSaves();
        stubNotificationSuccess();

        settlementService.settleTransaction(transaction);

        assertEquals(Transaction.Status.DECLINED, transaction.getStatus());
        assertEquals(money("6000.00"), senderWallet.getBalance());
        assertEquals(money("0.00"), senderWallet.getDailySpent());
        assertNotNull(transaction.getSettledAt());
        verify(walletRepository).save(senderWallet);

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(captor.capture());
        LedgerEntry entry = captor.getValue();
        assertEquals(LedgerEntry.Type.CREDIT, entry.getType());
        assertSame(senderWallet, entry.getWallet());
        assertEquals(money("5000.00"), entry.getBalanceBefore());
        assertEquals(money("6000.00"), entry.getBalanceAfter());
        verify(notificationService).create(
                eq(1L), eq(Notification.Type.TRANSFER_FAILED),
                eq("Transfer Failed"), contains("refunded"), same(transaction));
    }

    @Test
    void settleTransaction_inactiveReceiver_declinesAndRefundsSender() throws Exception {
        receiverWallet.setStatus(Wallet.Status.INACTIVE);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        stubSaves();
        stubNotificationSuccess();

        settlementService.settleTransaction(transaction);

        assertEquals(Transaction.Status.DECLINED, transaction.getStatus());
        assertEquals(money("6000.00"), senderWallet.getBalance());
        assertEquals(money("0.00"), senderWallet.getDailySpent());
        verify(walletRepository).save(senderWallet);
        verify(ledgerEntryRepository).save(any(LedgerEntry.class));
    }

    @Test
    void settleTransaction_missingTransaction_wrapsNotFoundException() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class,
                () -> settlementService.settleTransaction(transaction));

        assertTrue(ex.getMessage().contains("Error settling transaction 1"));
        assertInstanceOf(ResponseStatusException.class, ex.getCause());
        assertEquals(404, ((ResponseStatusException) ex.getCause()).getStatusCode().value());
        verifyNoInteractions(walletRepository, ledgerEntryRepository, notificationService);
    }

    @Test
    void settleTransaction_nullReceiverWallet_wrapsException() {
        transaction.setReceiverWallet(null);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        Exception ex = assertThrows(Exception.class,
                () -> settlementService.settleTransaction(transaction));

        assertTrue(ex.getMessage().contains("Receiver wallet not found"));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void settleTransaction_nullSenderWallet_wrapsException() {
        transaction.setSenderWallet(null);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        Exception ex = assertThrows(Exception.class,
                () -> settlementService.settleTransaction(transaction));

        assertTrue(ex.getMessage().contains("Sender wallet not found"));
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
    }

    @Test
    void settleTransaction_nullReceiverStatus_skipsWalletTransferButSavesTransaction() throws Exception {
        receiverWallet.setStatus(null);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        settlementService.settleTransaction(transaction);

        assertEquals(Transaction.Status.PENDING, transaction.getStatus());
        assertNotNull(transaction.getSettledAt());
        verify(transactionRepository).save(transaction);
        verifyNoInteractions(walletRepository, ledgerEntryRepository, notificationService);
    }

    @Test
    void settleTransaction_senderNotificationFailure_doesNotPreventReceiverNotification() throws Exception {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        stubSaves();
        doThrow(new RuntimeException("sender notification failure"))
                .when(notificationService).create(
                        eq(1L), eq(Notification.Type.TRANSFER_SETTLED),
                        eq("Transfer Settled"), anyString(), same(transaction));
        when(notificationService.create(
                eq(2L), eq(Notification.Type.TRANSFER_SETTLED),
                eq("Transfer Received"), anyString(), same(transaction)))
                .thenReturn(new Notification());

        assertDoesNotThrow(() -> settlementService.settleTransaction(transaction));

        assertEquals(Transaction.Status.SUCCESS, transaction.getStatus());
        assertEquals(money("3000.00"), receiverWallet.getBalance());
        verify(notificationService).create(
                eq(2L), eq(Notification.Type.TRANSFER_SETTLED),
                eq("Transfer Received"), anyString(), same(transaction));
    }

    @Test
    void settleTransaction_receiverNotificationFailure_doesNotUndoSettlement() throws Exception {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        stubSaves();
        when(notificationService.create(
                eq(1L), eq(Notification.Type.TRANSFER_SETTLED),
                eq("Transfer Settled"), anyString(), same(transaction)))
                .thenReturn(new Notification());
        doThrow(new RuntimeException("receiver notification failure"))
                .when(notificationService).create(
                        eq(2L), eq(Notification.Type.TRANSFER_SETTLED),
                        eq("Transfer Received"), anyString(), same(transaction));

        assertDoesNotThrow(() -> settlementService.settleTransaction(transaction));

        assertEquals(Transaction.Status.SUCCESS, transaction.getStatus());
        assertEquals(money("3000.00"), receiverWallet.getBalance());
        verify(transactionRepository).save(transaction);
    }

    @Test
    void settleTransaction_declinedNotificationFailure_doesNotUndoRefund() throws Exception {
        receiverWallet.setStatus(Wallet.Status.FROZEN);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        stubSaves();
        doThrow(new RuntimeException("notification failure"))
                .when(notificationService).create(
                        eq(1L), eq(Notification.Type.TRANSFER_FAILED),
                        eq("Transfer Failed"), anyString(), same(transaction));

        assertDoesNotThrow(() -> settlementService.settleTransaction(transaction));

        assertEquals(Transaction.Status.DECLINED, transaction.getStatus());
        assertEquals(money("6000.00"), senderWallet.getBalance());
        verify(transactionRepository).save(transaction);
    }

    @Test
    void markFailedAndRefund_transactionNotFound_doesNothing() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> settlementService.markFailedAndRefund(transaction));

        verify(transactionRepository, never()).save(any(Transaction.class));
        verifyNoInteractions(walletRepository, ledgerEntryRepository, notificationService);
    }

    @Test
    void markFailedAndRefund_transactionNotPending_doesNothing() {
        transaction.setStatus(Transaction.Status.SUCCESS);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        settlementService.markFailedAndRefund(transaction);

        verify(transactionRepository, never()).save(any(Transaction.class));
        verifyNoInteractions(walletRepository, ledgerEntryRepository, notificationService);
    }

    @Test
    void markFailedAndRefund_pendingTransaction_refundsSenderAndWritesLedger() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        stubSaves();
        stubNotificationSuccess();

        settlementService.markFailedAndRefund(transaction);

        assertEquals(Transaction.Status.FAILED, transaction.getStatus());
        assertNotNull(transaction.getSettledAt());
        assertEquals(money("6000.00"), senderWallet.getBalance());
        assertEquals(money("0.00"), senderWallet.getDailySpent());

        verify(transactionRepository).save(transaction);
        verify(walletRepository).save(senderWallet);

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(captor.capture());
        LedgerEntry entry = captor.getValue();
        assertSame(transaction, entry.getTransaction());
        assertSame(senderWallet, entry.getWallet());
        assertEquals(LedgerEntry.Type.CREDIT, entry.getType());
        assertEquals(money("5000.00"), entry.getBalanceBefore());
        assertEquals(money("6000.00"), entry.getBalanceAfter());
        verify(notificationService).create(
                eq(1L), eq(Notification.Type.TRANSFER_FAILED),
                eq("Transfer Failed"), contains("refunded"), same(transaction));
    }

    @Test
    void markFailedAndRefund_dailySpentLessThanAmount_remainsUnchanged() {
        senderWallet.setDailySpent(new BigDecimal("200.00"));

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(transaction));

        stubSaves();

        settlementService.markFailedAndRefund(transaction);

        // Existing service leaves dailySpent unchanged in this case.
        assertEquals(
                0,
                new BigDecimal("200.00").compareTo(senderWallet.getDailySpent())
        );

        assertEquals(
                0,
                new BigDecimal("6000.00").compareTo(senderWallet.getBalance())
        );

        assertEquals(Transaction.Status.FAILED, transaction.getStatus());

        verify(transactionRepository).save(transaction);
        verify(walletRepository).save(senderWallet);
        verify(ledgerEntryRepository).save(any(LedgerEntry.class));
    }

    @Test
    void markFailedAndRefund_nullSenderStillMarksTransactionFailed() {
        transaction.setSenderWallet(null);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        settlementService.markFailedAndRefund(transaction);

        assertEquals(Transaction.Status.FAILED, transaction.getStatus());
        assertNotNull(transaction.getSettledAt());
        verify(transactionRepository).save(transaction);
        verifyNoInteractions(walletRepository, ledgerEntryRepository, notificationService);
    }

    @Test
    void markFailedAndRefund_notificationFailureDoesNotUndoRefund() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        stubSaves();
        doThrow(new RuntimeException("notification unavailable"))
                .when(notificationService).create(
                        eq(1L), eq(Notification.Type.TRANSFER_FAILED),
                        eq("Transfer Failed"), anyString(), same(transaction));

        assertDoesNotThrow(() -> settlementService.markFailedAndRefund(transaction));

        assertEquals(Transaction.Status.FAILED, transaction.getStatus());
        assertEquals(money("6000.00"), senderWallet.getBalance());
        verify(walletRepository).save(senderWallet);
        verify(ledgerEntryRepository).save(any(LedgerEntry.class));
    }
}
