package com.p2ppayment.service;

import com.p2ppayment.entity.FraudAlert;
import com.p2ppayment.entity.LedgerEntry;
import com.p2ppayment.entity.Notification;
import com.p2ppayment.entity.Transaction;
import com.p2ppayment.entity.User;
import com.p2ppayment.entity.Wallet;
import com.p2ppayment.repository.FraudAlertRepository;
import com.p2ppayment.repository.LedgerEntryRepository;
import com.p2ppayment.repository.TransactionRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudServiceTest {

    @Mock private FraudAlertRepository fraudAlertRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private LedgerEntryRepository ledgerEntryRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks private FraudService fraudService;

    private Transaction transaction;
    private FraudAlert alert;
    private Wallet senderWallet;
    private Wallet receiverWallet;
    private User sender;
    private User receiver;

    private static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setId(1L);
        sender.setName("Sender");
        sender.setEmail("sender@example.com");
        sender.setRole(User.Role.USER);

        receiver = new User();
        receiver.setId(2L);
        receiver.setName("Recipient");
        receiver.setEmail("recipient@example.com");
        receiver.setRole(User.Role.USER);

        senderWallet = new Wallet();
        senderWallet.setId(1L);
        senderWallet.setUser(sender);
        senderWallet.setBalance(amount("90000.00"));
        senderWallet.setDailySpent(amount("10000.00"));
        senderWallet.setStatus(Wallet.Status.ACTIVE);

        receiverWallet = new Wallet();
        receiverWallet.setId(2L);
        receiverWallet.setUser(receiver);
        receiverWallet.setBalance(amount("1000.00"));
        receiverWallet.setDailySpent(amount("0.00"));
        receiverWallet.setStatus(Wallet.Status.ACTIVE);

        transaction = new Transaction();
        transaction.setId(10L);
        transaction.setSenderWallet(senderWallet);
        transaction.setReceiverWallet(receiverWallet);
        transaction.setAmount(amount("8000.00"));
        transaction.setType(Transaction.Type.P2P_TRANSFER);
        transaction.setStatus(Transaction.Status.PENDING);

        alert = new FraudAlert();
        alert.setId(20L);
        alert.setTransaction(transaction);
        alert.setAlertType(FraudAlert.AlertType.HIGH_VALUE);
        alert.setStatus(FraudAlert.Status.PENDING);
        alert.setNotes("High value transfer");
    }

    @Test
    void checkDailyLimit_withinLimit_returnsTrue() {
        assertTrue(fraudService.checkDailyLimit(1L, amount("10000"), amount("20000")));
    }

    @Test
    void checkDailyLimit_exactlyAtLimit_returnsTrue() {
        assertTrue(fraudService.checkDailyLimit(1L, amount("45000"), amount("5000")));
    }

    @Test
    void checkDailyLimit_overLimit_returnsFalse() {
        assertFalse(fraudService.checkDailyLimit(1L, amount("45000"), amount("5000.01")));
    }

    @Test
    void checkDuplicate_noRecentDuplicates_returnsTrue() {
        when(transactionRepository.findRecentDuplicates(eq(1L), eq(2L), eq(amount("100.00")), any()))
                .thenReturn(List.of());

        assertTrue(fraudService.checkDuplicate(1L, 2L, amount("100.00")));
        verify(transactionRepository).findRecentDuplicates(eq(1L), eq(2L), eq(amount("100.00")), any());
    }

    @Test
    void checkDuplicate_recentDuplicateExists_returnsFalse() {
        when(transactionRepository.findRecentDuplicates(eq(1L), eq(2L), eq(amount("100.00")), any()))
                .thenReturn(List.of(transaction));

        assertFalse(fraudService.checkDuplicate(1L, 2L, amount("100.00")));
    }

    @Test
    void isHighValue_atThreshold_returnsTrue() {
        assertTrue(fraudService.isHighValue(amount("8000.00")));
    }

    @Test
    void isHighValue_aboveThreshold_returnsTrue() {
        assertTrue(fraudService.isHighValue(amount("8000.01")));
    }

    @Test
    void isHighValue_belowThreshold_returnsFalse() {
        assertFalse(fraudService.isHighValue(amount("7999.99")));
    }

    @Test
    void createAlert_savesPendingAlertWithSuppliedDetails() {
        when(fraudAlertRepository.save(any(FraudAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FraudAlert result = fraudService.createAlert(
                transaction, FraudAlert.AlertType.HIGH_VALUE, "High value transfer");

        assertNotNull(result);
        assertSame(transaction, result.getTransaction());
        assertEquals(FraudAlert.AlertType.HIGH_VALUE, result.getAlertType());
        assertEquals(FraudAlert.Status.PENDING, result.getStatus());
        assertEquals("High value transfer", result.getNotes());
        assertNotNull(result.getCreatedAt());
        verify(fraudAlertRepository).save(result);
    }

    @Test
    void blockAlert_alertNotFound_throwsNotFound() {
        when(fraudAlertRepository.findById(20L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> fraudService.blockAlert(20L, "Blocked"));

        assertEquals(404, ex.getStatusCode().value());
        verifyNoInteractions(walletRepository, ledgerEntryRepository, notificationService);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void blockAlert_alertAlreadyBlocked_throwsConflict() {
        alert.setStatus(FraudAlert.Status.BLOCKED);
        when(fraudAlertRepository.findById(20L)).thenReturn(Optional.of(alert));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> fraudService.blockAlert(20L, "Blocked"));

        assertEquals(409, ex.getStatusCode().value());
        verify(transactionRepository, never()).save(any());
        verifyNoInteractions(walletRepository, ledgerEntryRepository, notificationService);
    }

    @Test
    void blockAlert_transactionNotPending_throwsConflict() {
        transaction.setStatus(Transaction.Status.FAILED);
        when(fraudAlertRepository.findById(20L)).thenReturn(Optional.of(alert));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> fraudService.blockAlert(20L, "Blocked"));

        assertEquals(409, ex.getStatusCode().value());
        verify(transactionRepository, never()).save(any());
        verifyNoInteractions(walletRepository, ledgerEntryRepository, notificationService);
    }

    @Test
    void blockAlert_senderWalletMissing_throwsNotFound() {
        transaction.setSenderWallet(null);
        when(fraudAlertRepository.findById(20L)).thenReturn(Optional.of(alert));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> fraudService.blockAlert(20L, "Blocked"));

        assertEquals(404, ex.getStatusCode().value());
        verify(transactionRepository, never()).save(any());
        verify(fraudAlertRepository, never()).save(any(FraudAlert.class));
        verifyNoInteractions(ledgerEntryRepository, notificationService);
    }

    @Test
    void blockAlert_success_refundsSenderAndUpdatesRecords() {
        when(fraudAlertRepository.findById(20L)).thenReturn(Optional.of(alert));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FraudAlert result = fraudService.blockAlert(20L, "Confirmed fraud");

        assertSame(alert, result);
        assertEquals(amount("98000.00"), senderWallet.getBalance());
        assertEquals(amount("2000.00"), senderWallet.getDailySpent());
        assertEquals(Transaction.Status.FAILED, transaction.getStatus());
        assertNotNull(transaction.getSettledAt());
        assertEquals(FraudAlert.Status.BLOCKED, alert.getStatus());
        assertEquals("Confirmed fraud", alert.getNotes());

        verify(walletRepository).save(senderWallet);
        verify(transactionRepository).save(transaction);
        verify(fraudAlertRepository).save(alert);

        ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(ledgerCaptor.capture());
        LedgerEntry ledger = ledgerCaptor.getValue();
        assertSame(transaction, ledger.getTransaction());
        assertSame(senderWallet, ledger.getWallet());
        assertEquals(LedgerEntry.Type.CREDIT, ledger.getType());
        assertEquals(amount("8000.00"), ledger.getAmount());
        assertEquals(amount("90000.00"), ledger.getBalanceBefore());
        assertEquals(amount("98000.00"), ledger.getBalanceAfter());

        verify(notificationService).create(
                eq(1L), eq(Notification.Type.TRANSFER_FAILED),
                eq("Transfer Blocked by Admin"), contains("refunded"),
                same(transaction));
        verify(notificationService).create(
                eq(2L), eq(Notification.Type.TRANSFER_FAILED),
                eq("Incoming Transfer Blocked"), contains("blocked"),
                same(transaction));
    }

    @Test
    void blockAlert_dailySpentLessThanRefundAmount_resetsDailySpentToZero() {
        senderWallet.setDailySpent(amount("100.00"));
        when(fraudAlertRepository.findById(20L)).thenReturn(Optional.of(alert));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        fraudService.blockAlert(20L, null);

        assertEquals(amount("98000.00"), senderWallet.getBalance());
        assertEquals(amount("0"), senderWallet.getDailySpent());
        assertEquals("High value transfer", alert.getNotes());
    }

    @Test
    void blockAlert_senderNotificationThrows_receiverNotificationStillAttempted() {
        when(fraudAlertRepository.findById(20L)).thenReturn(Optional.of(alert));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("notification unavailable"))
                .when(notificationService).create(
                        eq(1L), eq(Notification.Type.TRANSFER_FAILED),
                        eq("Transfer Blocked by Admin"), anyString(), same(transaction));

        assertDoesNotThrow(() -> fraudService.blockAlert(20L, "Confirmed fraud"));

        assertEquals(FraudAlert.Status.BLOCKED, alert.getStatus());
        assertEquals(Transaction.Status.FAILED, transaction.getStatus());
        verify(notificationService, times(2)).create(
                anyLong(), eq(Notification.Type.TRANSFER_FAILED), anyString(), anyString(), same(transaction));
    }

    @Test
    void blockAlert_receiverNotificationThrows_doesNotUndoBlock() {
        when(fraudAlertRepository.findById(20L)).thenReturn(Optional.of(alert));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("notification unavailable"))
                .when(notificationService).create(
                        eq(2L), eq(Notification.Type.TRANSFER_FAILED),
                        eq("Incoming Transfer Blocked"), anyString(), same(transaction));

        assertDoesNotThrow(() -> fraudService.blockAlert(20L, "Confirmed fraud"));

        assertEquals(FraudAlert.Status.BLOCKED, alert.getStatus());
        assertEquals(Transaction.Status.FAILED, transaction.getStatus());
        verify(walletRepository).save(senderWallet);
        verify(ledgerEntryRepository).save(any(LedgerEntry.class));
    }

    @Test
    void blockAlert_nullReceiverWallet_skipsReceiverNotification() {
        transaction.setReceiverWallet(null);
        when(fraudAlertRepository.findById(20L)).thenReturn(Optional.of(alert));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        fraudService.blockAlert(20L, "Confirmed fraud");

        verify(notificationService, times(1)).create(
                eq(1L), eq(Notification.Type.TRANSFER_FAILED), anyString(), anyString(), same(transaction));
        verify(notificationService, never()).create(
                eq(2L), eq(Notification.Type.TRANSFER_FAILED), anyString(), anyString(), same(transaction));
    }

    @Test
    void dismissAlert_alertNotFound_throwsNotFound() {
        when(fraudAlertRepository.findById(20L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> fraudService.dismissAlert(20L, "False positive"));

        assertEquals(404, ex.getStatusCode().value());
        verify(fraudAlertRepository, never()).save(any(FraudAlert.class));
    }

    @Test
    void dismissAlert_alreadyDismissed_throwsConflict() {
        alert.setStatus(FraudAlert.Status.DISMISSED);
        when(fraudAlertRepository.findById(20L)).thenReturn(Optional.of(alert));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> fraudService.dismissAlert(20L, "False positive"));

        assertEquals(409, ex.getStatusCode().value());
        verify(fraudAlertRepository, never()).save(any(FraudAlert.class));
    }

    @Test
    void dismissAlert_success_updatesStatusAndNotes() {
        when(fraudAlertRepository.findById(20L)).thenReturn(Optional.of(alert));
        when(fraudAlertRepository.save(any(FraudAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FraudAlert result = fraudService.dismissAlert(20L, "False positive");

        assertSame(alert, result);
        assertEquals(FraudAlert.Status.DISMISSED, result.getStatus());
        assertEquals("False positive", result.getNotes());
        assertEquals(Transaction.Status.PENDING, transaction.getStatus());
        verify(fraudAlertRepository).save(alert);
        verifyNoInteractions(transactionRepository, walletRepository, ledgerEntryRepository, notificationService);
    }

    @Test
    void dismissAlert_nullNotes_preservesExistingNotes() {
        when(fraudAlertRepository.findById(20L)).thenReturn(Optional.of(alert));
        when(fraudAlertRepository.save(any(FraudAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FraudAlert result = fraudService.dismissAlert(20L, null);

        assertEquals(FraudAlert.Status.DISMISSED, result.getStatus());
        assertEquals("High value transfer", result.getNotes());
    }
}
