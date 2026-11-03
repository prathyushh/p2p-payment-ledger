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

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SettlementService settlementService;

    private Transaction testTransaction;
    private Wallet senderWallet;
    private Wallet receiverWallet;
    private User sender;
    private User receiver;

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
        senderWallet.setBalance(new BigDecimal("5000.00"));
        senderWallet.setDailySpent(new BigDecimal("1000.00"));
        senderWallet.setStatus(Wallet.Status.ACTIVE);

        receiverWallet = new Wallet();
        receiverWallet.setId(2L);
        receiverWallet.setUser(receiver);
        receiverWallet.setBalance(new BigDecimal("2000.00"));
        receiverWallet.setDailySpent(BigDecimal.ZERO);
        receiverWallet.setStatus(Wallet.Status.ACTIVE);

        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setSenderWallet(senderWallet);
        testTransaction.setReceiverWallet(receiverWallet);
        testTransaction.setAmount(new BigDecimal("1000.00"));
        testTransaction.setType(Transaction.Type.P2P_TRANSFER);
        testTransaction.setStatus(Transaction.Status.PENDING);
        testTransaction.setSettlementDueDate(LocalDate.now());
    }

    private void mockSuccessfulSaves() {
        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(ledgerEntryRepository.save(any(LedgerEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(notificationService.create(
                anyLong(),
                any(Notification.Type.class),
                anyString(),
                anyString(),
                any(Transaction.class)
        )).thenReturn(new Notification());
    }

    @Test
    void getPendingTransactionsDue_ReturnsTransactions() {
        when(transactionRepository.findDueTransactions(any(LocalDate.class)))
                .thenReturn(List.of(testTransaction));

        List<Transaction> result =
                settlementService.getPendingTransactionsDue();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());

        verify(transactionRepository).findDueTransactions(any(LocalDate.class));
    }

    @Test
    void getPendingTransactionsDue_NoTransactions_ReturnsEmptyList() {
        when(transactionRepository.findDueTransactions(any(LocalDate.class)))
                .thenReturn(List.of());

        List<Transaction> result =
                settlementService.getPendingTransactionsDue();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void settleTransaction_Success_CreditsReceiver() throws Exception {
        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(testTransaction));

        mockSuccessfulSaves();

        settlementService.settleTransaction(testTransaction);

        assertEquals(
                0,
                new BigDecimal("3000.00")
                        .compareTo(receiverWallet.getBalance())
        );
        assertEquals(Transaction.Status.SUCCESS, testTransaction.getStatus());
        assertNotNull(testTransaction.getSettledAt());

        verify(walletRepository).save(receiverWallet);
        verify(ledgerEntryRepository).save(argThat(entry ->
                entry.getType() == LedgerEntry.Type.CREDIT
                        && entry.getWallet().getId().equals(2L)
                        && entry.getAmount().compareTo(
                                new BigDecimal("1000.00")) == 0
                        && entry.getBalanceBefore().compareTo(
                                new BigDecimal("2000.00")) == 0
                        && entry.getBalanceAfter().compareTo(
                                new BigDecimal("3000.00")) == 0
        ));
        verify(transactionRepository).save(testTransaction);
    }

    @Test
    void settleTransaction_AlreadySettled_DoesNotChangeWallets()
            throws Exception {
        testTransaction.setStatus(Transaction.Status.SUCCESS);

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(testTransaction));

        settleTransactionWithoutChanges();

        verify(walletRepository, never()).save(any(Wallet.class));
        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void settleTransaction_FrozenReceiver_DeclinesAndRefundsSender()
            throws Exception {
        receiverWallet.setStatus(Wallet.Status.FROZEN);

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(testTransaction));

        mockSuccessfulSaves();

        settlementService.settleTransaction(testTransaction);

        assertEquals(Transaction.Status.DECLINED, testTransaction.getStatus());
        assertEquals(
                0,
                new BigDecimal("6000.00")
                        .compareTo(senderWallet.getBalance())
        );
        assertEquals(
                0,
                new BigDecimal("0.00")
                        .compareTo(senderWallet.getDailySpent())
        );

        verify(walletRepository).save(senderWallet);
        verify(ledgerEntryRepository).save(argThat(entry ->
                entry.getType() == LedgerEntry.Type.CREDIT
                        && entry.getWallet().getId().equals(1L)
                        && entry.getBalanceBefore().compareTo(
                                new BigDecimal("5000.00")) == 0
                        && entry.getBalanceAfter().compareTo(
                                new BigDecimal("6000.00")) == 0
        ));
        verify(transactionRepository).save(testTransaction);
    }

    @Test
    void settleTransaction_InactiveReceiver_DeclinesAndRefundsSender()
            throws Exception {
        receiverWallet.setStatus(Wallet.Status.INACTIVE);

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(testTransaction));

        mockSuccessfulSaves();

        settlementService.settleTransaction(testTransaction);

        assertEquals(Transaction.Status.DECLINED, testTransaction.getStatus());
        assertEquals(
                0,
                new BigDecimal("6000.00")
                        .compareTo(senderWallet.getBalance())
        );

        verify(walletRepository).save(senderWallet);
        verify(ledgerEntryRepository).save(any(LedgerEntry.class));
    }

    @Test
    void settleTransaction_TransactionNotFound_ThrowsException() {
        when(transactionRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                Exception.class,
                () -> settlementService.settleTransaction(testTransaction)
        );

        verify(walletRepository, never()).save(any(Wallet.class));
        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
    }

    @Test
    void settleTransaction_NullReceiverWallet_ThrowsException() {
        testTransaction.setReceiverWallet(null);

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(testTransaction));

        assertThrows(
                Exception.class,
                () -> settlementService.settleTransaction(testTransaction)
        );
    }

    private void settleTransactionWithoutChanges() throws Exception {
        settlementService.settleTransaction(testTransaction);
    }
}