package com.p2ppayment.service;

import com.p2ppayment.model.*;
import com.p2ppayment.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

    @Test
    void getPendingTransactionsDue_Success() {
        // Arrange
        when(transactionRepository.findDueTransactions(any(LocalDate.class)))
            .thenReturn(Arrays.asList(testTransaction));

        // Act
        List<Transaction> result = settlementService.getPendingTransactionsDue();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTransaction.getId(), result.get(0).getId());
    }

    @Test
    void settleTransaction_Success() throws Exception {
        // Arrange
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));
        when(walletRepository.save(any(Wallet.class))).thenReturn(receiverWallet);
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        doNothing().when(notificationService).create(anyLong(), any(), anyString(), anyString(), any());

        // Act
        settlementService.settleTransaction(testTransaction);

        // Assert
        verify(walletRepository, times(1)).save(any(Wallet.class));
        verify(ledgerEntryRepository, times(1)).save(any(LedgerEntry.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void settleTransaction_AlreadySettled_NoAction() throws Exception {
        // Arrange
        testTransaction.setStatus(Transaction.Status.SUCCESS);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));

        // Act
        settlementService.settleTransaction(testTransaction);

        // Assert
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void settleTransaction_ReceiverWalletFrozen_DeclinedSettlement() throws Exception {
        // Arrange
        receiverWallet.setStatus(Wallet.Status.FROZEN);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));
        when(walletRepository.save(any(Wallet.class))).thenReturn(senderWallet);
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        doNothing().when(notificationService).create(anyLong(), any(), anyString(), anyString(), any());

        // Act
        settlementService.settleTransaction(testTransaction);

        // Assert
        verify(walletRepository, times(1)).save(any(Wallet.class)); // Refund to sender
        verify(ledgerEntryRepository, times(1)).save(any(LedgerEntry.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void settleTransaction_ReceiverWalletInactive_DeclinedSettlement() throws Exception {
        // Arrange
        receiverWallet.setStatus(Wallet.Status.INACTIVE);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));
        when(walletRepository.save(any(Wallet.class))).thenReturn(senderWallet);
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        doNothing().when(notificationService).create(anyLong(), any(), anyString(), anyString(), any());

        // Act
        settlementService.settleTransaction(testTransaction);

        // Assert
        verify(walletRepository, times(1)).save(any(Wallet.class)); // Refund to sender
        assertEquals(Transaction.Status.DECLINED, testTransaction.getStatus());
    }
}
