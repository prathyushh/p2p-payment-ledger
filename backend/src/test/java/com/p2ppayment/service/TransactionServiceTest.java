
package com.p2ppayment.service;

import com.p2ppayment.entity.LedgerEntry;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AutoReloadService autoReloadService;

    @Mock
    private FraudService fraudService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TransactionService transactionService;

    private User sender;
    private User recipient;
    private Wallet senderWallet;
    private Wallet recipientWallet;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setId(1L);
        sender.setEmail("sender@example.com");
        sender.setTransactionPin("hashedPin");

        recipient = new User();
        recipient.setId(2L);
        recipient.setEmail("recipient@example.com");

        senderWallet = new Wallet();
        senderWallet.setId(1L);
        senderWallet.setUser(sender);
        senderWallet.setBalance(new BigDecimal("10000.00"));
        senderWallet.setDailySpent(new BigDecimal("5000.00"));
        senderWallet.setStatus(Wallet.Status.ACTIVE);

        recipientWallet = new Wallet();
        recipientWallet.setId(2L);
        recipientWallet.setUser(recipient);
        recipientWallet.setBalance(new BigDecimal("1000.00"));
        recipientWallet.setDailySpent(BigDecimal.ZERO);
        recipientWallet.setStatus(Wallet.Status.ACTIVE);

        transaction = new Transaction();
        transaction.setId(1L);
        transaction.setSenderWallet(senderWallet);
        transaction.setReceiverWallet(recipientWallet);
        transaction.setAmount(new BigDecimal("500.00"));
        transaction.setType(Transaction.Type.P2P_TRANSFER);
        transaction.setStatus(Transaction.Status.PENDING);
    }

    private void mockValidSenderAndRecipient() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(sender));

        when(walletRepository.findByUser(sender))
                .thenReturn(Optional.of(senderWallet));

        when(passwordEncoder.matches("1234", "hashedPin"))
                .thenReturn(true);

        when(userRepository.findByEmail("recipient@example.com"))
                .thenReturn(Optional.of(recipient));

        when(walletRepository.findByUser(recipient))
                .thenReturn(Optional.of(recipientWallet));
    }

    private void mockValidTransferDependencies() {
        mockValidSenderAndRecipient();

        /*
         * IMPORTANT:
         * FraudService.checkDuplicate() returns true when there is
         * NO duplicate transaction.
         *
         * Returning false here would trigger the duplicate-transfer
         * exception in TransactionService.sendMoney().
         */
        when(fraudService.checkDuplicate(
                eq(1L),
                eq(2L),
                eq(new BigDecimal("500.00"))))
                .thenReturn(true);

        when(fraudService.isHighValue(any(BigDecimal.class)))
                .thenReturn(false);

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(ledgerEntryRepository.save(any(LedgerEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void sendMoney_InvalidAmount_ThrowsBadRequest() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.sendMoney(
                        1L,
                        "recipient@example.com",
                        BigDecimal.ZERO,
                        "Test payment",
                        "1234"
                )
        );

        assertEquals(400, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains(
                "Amount must be greater than 0"));

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void sendMoney_SenderNotFound_ThrowsNotFound() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.sendMoney(
                        1L,
                        "recipient@example.com",
                        new BigDecimal("500.00"),
                        "Test payment",
                        "1234"
                )
        );

        assertEquals(404, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Sender not found"));
    }

    @Test
    void sendMoney_InactiveSenderWallet_ThrowsConflict() {
        senderWallet.setStatus(Wallet.Status.INACTIVE);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(sender));

        when(walletRepository.findByUser(sender))
                .thenReturn(Optional.of(senderWallet));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.sendMoney(
                        1L,
                        "recipient@example.com",
                        new BigDecimal("500.00"),
                        "Test payment",
                        "1234"
                )
        );

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("not active"));
    }

    @Test
    void sendMoney_FrozenSenderWallet_ThrowsConflict() {
        senderWallet.setStatus(Wallet.Status.FROZEN);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(sender));

        when(walletRepository.findByUser(sender))
                .thenReturn(Optional.of(senderWallet));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.sendMoney(
                        1L,
                        "recipient@example.com",
                        new BigDecimal("500.00"),
                        "Test payment",
                        "1234"
                )
        );

        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    void sendMoney_MissingPin_ThrowsBadRequest() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(sender));

        when(walletRepository.findByUser(sender))
                .thenReturn(Optional.of(senderWallet));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.sendMoney(
                        1L,
                        "recipient@example.com",
                        new BigDecimal("500.00"),
                        "Test payment",
                        null
                )
        );

        assertEquals(400, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("PIN is required"));
    }

    @Test
    void sendMoney_InvalidPin_ThrowsUnauthorized() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(sender));

        when(walletRepository.findByUser(sender))
                .thenReturn(Optional.of(senderWallet));

        when(passwordEncoder.matches("1234", "hashedPin"))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.sendMoney(
                        1L,
                        "recipient@example.com",
                        new BigDecimal("500.00"),
                        "Test payment",
                        "1234"
                )
        );

        assertEquals(401, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Invalid PIN"));
    }

    @Test
    void sendMoney_RecipientNotFound_ThrowsNotFound() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(sender));

        when(walletRepository.findByUser(sender))
                .thenReturn(Optional.of(senderWallet));

        when(passwordEncoder.matches("1234", "hashedPin"))
                .thenReturn(true);

        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        when(userRepository.findByPhoneNumber("missing@example.com"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.sendMoney(
                        1L,
                        "missing@example.com",
                        new BigDecimal("500.00"),
                        "Test payment",
                        "1234"
                )
        );

        assertEquals(404, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Recipient not found"));
    }

    @Test
    void sendMoney_SelfTransfer_ThrowsBadRequest() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(sender));

        when(walletRepository.findByUser(sender))
                .thenReturn(Optional.of(senderWallet));

        when(passwordEncoder.matches("1234", "hashedPin"))
                .thenReturn(true);

        when(userRepository.findByEmail("sender@example.com"))
                .thenReturn(Optional.of(sender));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.sendMoney(
                        1L,
                        "sender@example.com",
                        new BigDecimal("500.00"),
                        "Test payment",
                        "1234"
                )
        );

        assertEquals(400, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains(
                "Cannot send money to yourself"));
    }

    @Test
    void sendMoney_RecipientWalletFrozen_ThrowsConflict() {
        recipientWallet.setStatus(Wallet.Status.FROZEN);
        mockValidSenderAndRecipient();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.sendMoney(
                        1L,
                        "recipient@example.com",
                        new BigDecimal("500.00"),
                        "Test payment",
                        "1234"
                )
        );

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains(
                "Recipient wallet is frozen"));
    }

    @Test
    void sendMoney_InsufficientBalance_ThrowsConflict() {
        senderWallet.setBalance(new BigDecimal("100.00"));
        mockValidSenderAndRecipient();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.sendMoney(
                        1L,
                        "recipient@example.com",
                        new BigDecimal("500.00"),
                        "Test payment",
                        "1234"
                )
        );

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Insufficient balance"));
    }

    @Test
    void sendMoney_Success_DeductsSenderAndCreatesPendingTransaction() {
        mockValidTransferDependencies();

        Transaction result = transactionService.sendMoney(
                1L,
                "recipient@example.com",
                new BigDecimal("500.00"),
                "Test payment",
                "1234"
        );

        assertNotNull(result);
        assertEquals(Transaction.Status.PENDING, result.getStatus());
        assertEquals(Transaction.Type.P2P_TRANSFER, result.getType());

        assertEquals(
                0,
                new BigDecimal("9500.00").compareTo(
                        senderWallet.getBalance())
        );

        assertEquals(
                0,
                new BigDecimal("5500.00").compareTo(
                        senderWallet.getDailySpent())
        );

        verify(transactionRepository).save(any(Transaction.class));
        verify(walletRepository).save(senderWallet);

        verify(ledgerEntryRepository).save(argThat(entry ->
                entry.getType() == LedgerEntry.Type.DEBIT
                        && entry.getWallet().getId().equals(1L)
                        && entry.getAmount().compareTo(
                                new BigDecimal("500.00")) == 0
                        && entry.getBalanceBefore().compareTo(
                                new BigDecimal("10000.00")) == 0
                        && entry.getBalanceAfter().compareTo(
                                new BigDecimal("9500.00")) == 0
        ));

        verify(autoReloadService).attemptReload(1L, false);
    }

    @Test
    void getTransactionHistory_ReturnsPagedTransactions() {
        Pageable pageable = PageRequest.of(0, 20);

        Page<Transaction> expectedPage =
                new PageImpl<>(List.of(transaction), pageable, 1);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(sender));

        when(walletRepository.findByUser(sender))
                .thenReturn(Optional.of(senderWallet));

        when(transactionRepository.findByWallet(1L, pageable))
                .thenReturn(expectedPage);

        Page<Transaction> result =
                transactionService.getTransactionHistory(1L, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getId());
    }

    @Test
    void getMiniStatement_ReturnsRecentTransactions() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(sender));

        when(walletRepository.findByUser(sender))
                .thenReturn(Optional.of(senderWallet));

        when(transactionRepository.findMiniStatement(
                eq(1L), any(Pageable.class)))
                .thenReturn(List.of(transaction));

        List<Transaction> result =
                transactionService.getMiniStatement(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void getTransactionDetail_Success() {
        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(transaction));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(sender));

        when(walletRepository.findByUser(sender))
                .thenReturn(Optional.of(senderWallet));

        Transaction result =
                transactionService.getTransactionDetail(1L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getTransactionDetail_Unauthorized_ThrowsForbidden() {
        User otherUser = new User();
        otherUser.setId(3L);

        Wallet otherWallet = new Wallet();
        otherWallet.setId(3L);
        otherWallet.setUser(otherUser);

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(transaction));

        when(userRepository.findById(3L))
                .thenReturn(Optional.of(otherUser));

        when(walletRepository.findByUser(otherUser))
                .thenReturn(Optional.of(otherWallet));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.getTransactionDetail(1L, 3L)
        );

        assertEquals(403, exception.getStatusCode().value());
    }
}
