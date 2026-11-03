package com.p2ppayment.service;

import com.p2ppayment.entity.*;
import com.p2ppayment.repository.*;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private UserRepository userRepository;
    @Mock private LedgerEntryRepository ledgerEntryRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AutoReloadService autoReloadService;
    @Mock private FraudService fraudService;
    @Mock private NotificationService notificationService;

    @InjectMocks private TransactionService transactionService;

    private User sender;
    private User recipient;
    private User admin;
    private Wallet senderWallet;
    private Wallet recipientWallet;
    private Transaction transaction;

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setId(1L);
        sender.setEmail("sender@example.com");
        sender.setTransactionPin("hashedPin");

        recipient = new User();
        recipient.setId(2L);
        recipient.setEmail("recipient@example.com");
        recipient.setPhoneNumber("9876543210");

        admin = new User();
        admin.setId(3L);
        admin.setEmail("admin@example.com");

        senderWallet = new Wallet();
        senderWallet.setId(1L);
        senderWallet.setUser(sender);
        senderWallet.setBalance(money("10000.00"));
        senderWallet.setDailySpent(money("5000.00"));
        senderWallet.setStatus(Wallet.Status.ACTIVE);

        recipientWallet = new Wallet();
        recipientWallet.setId(2L);
        recipientWallet.setUser(recipient);
        recipientWallet.setBalance(money("1000.00"));
        recipientWallet.setDailySpent(BigDecimal.ZERO);
        recipientWallet.setStatus(Wallet.Status.ACTIVE);

        transaction = new Transaction();
        transaction.setId(10L);
        transaction.setSenderWallet(senderWallet);
        transaction.setReceiverWallet(recipientWallet);
        transaction.setAmount(money("500.00"));
        transaction.setType(Transaction.Type.P2P_TRANSFER);
        transaction.setStatus(Transaction.Status.PENDING);
    }

    private void mockSender() {
        // These shared stubs are used by different validation paths. Some paths
        // intentionally stop before every dependency is reached, so keep only
        // this reusable setup lenient; test-specific stubs remain strict.
        lenient().when(userRepository.findById(1L))
                .thenReturn(Optional.of(sender));
        lenient().when(walletRepository.findByUser(sender))
                .thenReturn(Optional.of(senderWallet));
        lenient().when(passwordEncoder.matches("1234", "hashedPin"))
                .thenReturn(true);
    }

    private void mockValidTransfer() {
        mockSender();
        when(userRepository.findByEmail("recipient@example.com")).thenReturn(Optional.of(recipient));
        when(walletRepository.findByUser(recipient)).thenReturn(Optional.of(recipientWallet));
        when(fraudService.checkDuplicate(eq(1L), eq(2L), any(BigDecimal.class))).thenReturn(true);
        when(fraudService.isHighValue(any(BigDecimal.class))).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private ResponseStatusException assertStatus(HttpStatus status, Runnable action) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(status, ex.getStatusCode());
        return ex;
    }

    @Test void sendMoney_nullAmount_isBadRequest() {
        assertStatus(HttpStatus.BAD_REQUEST, () ->
                transactionService.sendMoney(1L, "recipient@example.com", null, "test", "1234"));
        verifyNoInteractions(userRepository, walletRepository);
    }

    @Test void sendMoney_zeroAmount_isBadRequest() {
        assertStatus(HttpStatus.BAD_REQUEST, () ->
                transactionService.sendMoney(1L, "recipient@example.com", BigDecimal.ZERO, "test", "1234"));
    }

    @Test void sendMoney_negativeAmount_isBadRequest() {
        assertStatus(HttpStatus.BAD_REQUEST, () ->
                transactionService.sendMoney(1L, "recipient@example.com", money("-1"), "test", "1234"));
    }

    @Test void sendMoney_senderNotFound_isNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertStatus(HttpStatus.NOT_FOUND, () ->
                transactionService.sendMoney(1L, "recipient@example.com", money("500"), "test", "1234"));
    }

    @Test void sendMoney_senderWalletMissing_isNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.empty());
        assertStatus(HttpStatus.NOT_FOUND, () ->
                transactionService.sendMoney(1L, "recipient@example.com", money("500"), "test", "1234"));
    }

    @Test void sendMoney_inactiveSenderWallet_isConflict() {
        senderWallet.setStatus(Wallet.Status.INACTIVE);
        mockSender();
        assertStatus(HttpStatus.CONFLICT, () ->
                transactionService.sendMoney(1L, "recipient@example.com", money("500"), "test", "1234"));
    }

    @Test void sendMoney_frozenSenderWallet_isConflict() {
        senderWallet.setStatus(Wallet.Status.FROZEN);
        mockSender();
        assertStatus(HttpStatus.CONFLICT, () ->
                transactionService.sendMoney(1L, "recipient@example.com", money("500"), "test", "1234"));
    }

    @Test void sendMoney_nullPin_isBadRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        assertStatus(HttpStatus.BAD_REQUEST, () ->
                transactionService.sendMoney(1L, "recipient@example.com", money("500"), "test", null));
    }

    @Test void sendMoney_emptyPin_isBadRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        assertStatus(HttpStatus.BAD_REQUEST, () ->
                transactionService.sendMoney(1L, "recipient@example.com", money("500"), "test", ""));
    }

    @Test void sendMoney_wrongPin_isUnauthorized() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        when(passwordEncoder.matches("0000", "hashedPin")).thenReturn(false);
        assertStatus(HttpStatus.UNAUTHORIZED, () ->
                transactionService.sendMoney(1L, "recipient@example.com", money("500"), "test", "0000"));
    }

    @Test void sendMoney_findsRecipientByPhone_whenEmailDoesNotMatch() {
        mockSender();
        when(userRepository.findByEmail("9876543210")).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber("9876543210")).thenReturn(Optional.of(recipient));
        when(walletRepository.findByUser(recipient)).thenReturn(Optional.of(recipientWallet));
        when(fraudService.checkDuplicate(1L, 2L, money("500"))).thenReturn(true);
        when(fraudService.isHighValue(any())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction result = transactionService.sendMoney(1L, "9876543210", money("500"), "phone transfer", "1234");

        assertNotNull(result);
        verify(userRepository).findByPhoneNumber("9876543210");
    }

    @Test void sendMoney_recipientNotFound_isNotFound() {
        mockSender();
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber("missing@example.com")).thenReturn(Optional.empty());
        assertStatus(HttpStatus.NOT_FOUND, () ->
                transactionService.sendMoney(1L, "missing@example.com", money("500"), "test", "1234"));
    }

    @Test void sendMoney_selfTransfer_isBadRequest() {
        mockSender();
        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(sender));
        assertStatus(HttpStatus.BAD_REQUEST, () ->
                transactionService.sendMoney(1L, "sender@example.com", money("500"), "test", "1234"));
    }

    @Test void sendMoney_recipientWalletMissing_isNotFound() {
        mockSender();
        when(userRepository.findByEmail("recipient@example.com")).thenReturn(Optional.of(recipient));
        when(walletRepository.findByUser(recipient)).thenReturn(Optional.empty());
        assertStatus(HttpStatus.NOT_FOUND, () ->
                transactionService.sendMoney(1L, "recipient@example.com", money("500"), "test", "1234"));
    }

    @Test void sendMoney_frozenRecipientWallet_isConflict() {
        recipientWallet.setStatus(Wallet.Status.FROZEN);
        mockSender();
        when(userRepository.findByEmail("recipient@example.com")).thenReturn(Optional.of(recipient));
        when(walletRepository.findByUser(recipient)).thenReturn(Optional.of(recipientWallet));
        assertStatus(HttpStatus.CONFLICT, () ->
                transactionService.sendMoney(1L, "recipient@example.com", money("500"), "test", "1234"));
    }

    @Test void sendMoney_inactiveRecipientWallet_isAllowedByCurrentImplementation() {
        recipientWallet.setStatus(Wallet.Status.INACTIVE);
        mockValidTransfer();
        Transaction result = transactionService.sendMoney(1L, "recipient@example.com", money("500.00"), "test", "1234");
        assertNotNull(result);
    }

    @Test void sendMoney_insufficientBalance_isConflict() {
        senderWallet.setBalance(money("100"));
        mockSender();
        when(userRepository.findByEmail("recipient@example.com")).thenReturn(Optional.of(recipient));
        when(walletRepository.findByUser(recipient)).thenReturn(Optional.of(recipientWallet));
        assertStatus(HttpStatus.CONFLICT, () ->
                transactionService.sendMoney(1L, "recipient@example.com", money("500"), "test", "1234"));
    }

    @Test void sendMoney_dailyLimitExceeded_createsAlertAndBlocksTransfer() {
        senderWallet.setDailySpent(money("49900"));
        mockSender();
        when(userRepository.findByEmail("recipient@example.com")).thenReturn(Optional.of(recipient));
        when(walletRepository.findByUser(recipient)).thenReturn(Optional.of(recipientWallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findAllAdminUsers()).thenReturn(List.of(admin));

        ResponseStatusException ex = assertStatus(HttpStatus.UNPROCESSABLE_ENTITY, () ->
                transactionService.sendMoney(1L, "recipient@example.com", money("500"), "limit", "1234"));

        assertTrue(ex.getReason().contains("Daily send limit"));
        verify(fraudService).createAlert(any(Transaction.class),
                eq(FraudAlert.AlertType.DAILY_LIMIT_EXCEEDED), contains("Daily limit exceeded"));
        verify(notificationService, atLeastOnce()).create(anyLong(), any(), anyString(), anyString(), any());
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test void sendMoney_dailyLimitAlert_survivesNullRemark() {
        mockSender();
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        when(userRepository.findAllAdminUsers()).thenReturn(List.of());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.createDailyLimitFraudAlert(sender, recipientWallet, money("600"),
                money("49900"), money("50500"), null);

        verify(transactionRepository).save(argThat(t -> "Daily limit exceeded".equals(t.getRemark())
                && t.getStatus() == Transaction.Status.FAILED));
    }

    @Test void sendMoney_duplicateDetected_createsAlertAndBlocksTransfer() {
        mockSender();
        when(userRepository.findByEmail("recipient@example.com")).thenReturn(Optional.of(recipient));
        when(walletRepository.findByUser(recipient)).thenReturn(Optional.of(recipientWallet));
        when(fraudService.checkDuplicate(1L, 2L, money("500"))).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findAllAdminUsers()).thenReturn(List.of(admin));

        ResponseStatusException ex = assertStatus(HttpStatus.UNPROCESSABLE_ENTITY, () ->
                transactionService.sendMoney(1L, "recipient@example.com", money("500"), "duplicate", "1234"));

        assertTrue(ex.getReason().contains("Duplicate transfer detected"));
        verify(fraudService).createAlert(any(Transaction.class),
                eq(FraudAlert.AlertType.DUPLICATE_TRANSACTION), contains("Duplicate transaction detected"));
        verify(notificationService, atLeastOnce()).create(anyLong(), any(), anyString(), anyString(), any());
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test void sendMoney_duplicateAlert_nullRemarkUsesDefault() {
        mockSender();
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        when(userRepository.findAllAdminUsers()).thenReturn(List.of());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.createDuplicateFraudAlert(sender, recipient, recipientWallet, money("500"), null);

        verify(transactionRepository).save(argThat(t ->
                "Duplicate transaction detected".equals(t.getRemark()) && t.getStatus() == Transaction.Status.FAILED));
    }

    @Test void sendMoney_success_updatesWalletLedgerAndAutoReload() {
        mockValidTransfer();

        Transaction result = transactionService.sendMoney(1L, "recipient@example.com",
                money("500"), "test payment", "1234");

        assertNotNull(result);
        assertEquals(Transaction.Status.PENDING, result.getStatus());
        assertEquals(Transaction.Type.P2P_TRANSFER, result.getType());
        assertEquals(0, money("9500").compareTo(senderWallet.getBalance()));
        assertEquals(0, money("5500").compareTo(senderWallet.getDailySpent()));
        assertNotNull(result.getRequestId());
        assertNotNull(result.getSettlementDueDate());
        verify(ledgerEntryRepository).save(argThat(entry ->
                entry.getType() == LedgerEntry.Type.DEBIT
                        && entry.getWallet().getId().equals(1L)
                        && entry.getAmount().compareTo(money("500")) == 0
                        && entry.getBalanceBefore().compareTo(money("10000")) == 0
                        && entry.getBalanceAfter().compareTo(money("9500")) == 0));
        verify(autoReloadService).attemptReload(1L, false);
    }

    @Test void sendMoney_highValueTransfer_createsAlertAndNotifiesAdmins() {
        mockSender();
        when(userRepository.findByEmail("recipient@example.com")).thenReturn(Optional.of(recipient));
        when(walletRepository.findByUser(recipient)).thenReturn(Optional.of(recipientWallet));
        when(fraudService.checkDuplicate(1L, 2L, money("9000"))).thenReturn(true);
        when(fraudService.isHighValue(money("9000"))).thenReturn(true);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction saved = inv.getArgument(0);
            saved.setId(44L);
            return saved;
        });
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findAllAdminUsers()).thenReturn(List.of(admin));

        Transaction result = transactionService.sendMoney(1L, "recipient@example.com",
                money("9000"), "large transfer", "1234");

        assertEquals(44L, result.getId());
        verify(fraudService).createAlert(eq(result), eq(FraudAlert.AlertType.HIGH_VALUE), contains("High-value transfer"));
        verify(notificationService).create(eq(3L), eq(Notification.Type.FRAUD_ALERT_HIGH_VALUE),
                eq("High-Value Transfer Alert"), contains("Transaction ID: 44"), eq(result));
    }

    @Test void sendMoney_highValueAdminLookupFailure_doesNotFailTransfer() {
        mockSender();
        when(userRepository.findByEmail("recipient@example.com")).thenReturn(Optional.of(recipient));
        when(walletRepository.findByUser(recipient)).thenReturn(Optional.of(recipientWallet));
        when(fraudService.checkDuplicate(1L, 2L, money("9000"))).thenReturn(true);
        when(fraudService.isHighValue(money("9000"))).thenReturn(true);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findAllAdminUsers()).thenThrow(new RuntimeException("temporary failure"));

        assertDoesNotThrow(() -> transactionService.sendMoney(1L, "recipient@example.com",
                money("9000"), "large transfer", "1234"));
    }

    @Test void getTransactionHistory_returnsPagedResults() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Transaction> page = new PageImpl<>(List.of(transaction), pageable, 1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        when(transactionRepository.findByWallet(1L, pageable)).thenReturn(page);

        Page<Transaction> result = transactionService.getTransactionHistory(1L, 0, 20);

        assertEquals(1, result.getTotalElements());
        assertSame(transaction, result.getContent().get(0));
    }

    @Test void getTransactionHistory_missingUser_isNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertStatus(HttpStatus.NOT_FOUND, () -> transactionService.getTransactionHistory(99L, 0, 20));
    }

    @Test void getTransactionHistory_missingWallet_isNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.empty());
        assertStatus(HttpStatus.NOT_FOUND, () -> transactionService.getTransactionHistory(1L, 0, 20));
    }

    @Test void getMiniStatement_returnsTenMostRecentQueryResults() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        when(transactionRepository.findMiniStatement(eq(1L), any(Pageable.class))).thenReturn(List.of(transaction));

        List<Transaction> result = transactionService.getMiniStatement(1L);

        assertEquals(1, result.size());
        verify(transactionRepository).findMiniStatement(eq(1L), eq(PageRequest.of(0, 10)));
    }

    @Test void getMiniStatement_missingUser_isNotFound() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        assertStatus(HttpStatus.NOT_FOUND, () -> transactionService.getMiniStatement(9L));
    }

    @Test void getMiniStatement_missingWallet_isNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.empty());
        assertStatus(HttpStatus.NOT_FOUND, () -> transactionService.getMiniStatement(1L));
    }

    @Test void getTransactionDetail_senderCanView() {
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));

        assertSame(transaction, transactionService.getTransactionDetail(10L, 1L));
    }

    @Test void getTransactionDetail_receiverCanView() {
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));
        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
        when(walletRepository.findByUser(recipient)).thenReturn(Optional.of(recipientWallet));

        assertSame(transaction, transactionService.getTransactionDetail(10L, 2L));
    }

    @Test void getTransactionDetail_missingTransaction_isNotFound() {
        when(transactionRepository.findById(404L)).thenReturn(Optional.empty());
        assertStatus(HttpStatus.NOT_FOUND, () -> transactionService.getTransactionDetail(404L, 1L));
    }

    @Test void getTransactionDetail_missingUser_isNotFound() {
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertStatus(HttpStatus.NOT_FOUND, () -> transactionService.getTransactionDetail(10L, 99L));
    }

    @Test void getTransactionDetail_missingWallet_isNotFound() {
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.empty());
        assertStatus(HttpStatus.NOT_FOUND, () -> transactionService.getTransactionDetail(10L, 1L));
    }

    @Test void getTransactionDetail_otherUser_isForbidden() {
        User other = new User();
        other.setId(4L);
        Wallet otherWallet = new Wallet();
        otherWallet.setId(4L);
        otherWallet.setUser(other);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));
        when(userRepository.findById(4L)).thenReturn(Optional.of(other));
        when(walletRepository.findByUser(other)).thenReturn(Optional.of(otherWallet));

        assertStatus(HttpStatus.FORBIDDEN, () -> transactionService.getTransactionDetail(10L, 4L));
    }

    @Test void getTransactionStats_returnsRepositoryValues() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        when(transactionRepository.countSentTransactions(1L)).thenReturn(2L);
        when(transactionRepository.countReceivedTransactions(1L)).thenReturn(3L);
        when(transactionRepository.sumSentAmount(1L)).thenReturn(money("1000"));
        when(transactionRepository.sumReceivedAmount(1L)).thenReturn(money("2000"));
        when(transactionRepository.countSentThisMonth(1L)).thenReturn(1L);
        when(transactionRepository.countReceivedThisMonth(1L)).thenReturn(2L);
        when(transactionRepository.sumSentThisMonth(1L)).thenReturn(money("400"));
        when(transactionRepository.sumReceivedThisMonth(1L)).thenReturn(money("800"));

        TransactionService.TransactionStats stats = transactionService.getTransactionStats(1L);

        assertEquals(2L, stats.totalSent);
        assertEquals(3L, stats.totalReceived);
        assertEquals(0, money("1000").compareTo(stats.totalSentAmount));
        assertEquals(0, money("2000").compareTo(stats.totalReceivedAmount));
        assertEquals(1L, stats.thisMonthSent);
        assertEquals(2L, stats.thisMonthReceived);
        assertEquals(0, money("400").compareTo(stats.thisMonthSentAmount));
        assertEquals(0, money("800").compareTo(stats.thisMonthReceivedAmount));
    }

    @Test void getTransactionStats_nullAggregatesBecomeZero() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));

        TransactionService.TransactionStats stats = transactionService.getTransactionStats(1L);

        assertEquals(0L, stats.totalSent);
        assertEquals(0L, stats.totalReceived);
        assertEquals(BigDecimal.ZERO, stats.totalSentAmount);
        assertEquals(BigDecimal.ZERO, stats.totalReceivedAmount);
        assertEquals(BigDecimal.ZERO, stats.thisMonthSentAmount);
        assertEquals(BigDecimal.ZERO, stats.thisMonthReceivedAmount);
    }

    @Test void getTransactionStats_missingUser_isNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertStatus(HttpStatus.NOT_FOUND, () -> transactionService.getTransactionStats(99L));
    }

    @Test void getTransactionStats_missingWallet_isNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.empty());
        assertStatus(HttpStatus.NOT_FOUND, () -> transactionService.getTransactionStats(1L));
    }

    @Test void checkDuplicateTransaction_emptyResultMeansNotDuplicate() {
        when(transactionRepository.findRecentDuplicates(eq(1L), eq(2L), eq(money("500")), any()))
                .thenReturn(List.of());
        assertTrue(transactionService.checkDuplicateTransaction(1L, 2L, money("500")));
    }

    @Test void checkDuplicateTransaction_existingResultMeansDuplicate() {
        when(transactionRepository.findRecentDuplicates(eq(1L), eq(2L), eq(money("500")), any()))
                .thenReturn(List.of(transaction));
        assertFalse(transactionService.checkDuplicateTransaction(1L, 2L, money("500")));
    }

    @Test void dailyLimitAlert_notificationFailuresAreSwallowed() {
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findAllAdminUsers()).thenReturn(List.of(admin));
        doThrow(new RuntimeException("notification failed")).when(notificationService)
                .create(anyLong(), any(), anyString(), anyString(), any());

        assertDoesNotThrow(() -> transactionService.createDailyLimitFraudAlert(sender, recipientWallet,
                money("600"), money("49900"), money("50500"), "remark"));
    }

    @Test void duplicateAlert_notificationFailuresAreSwallowed() {
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findAllAdminUsers()).thenReturn(List.of(admin));
        doThrow(new RuntimeException("notification failed")).when(notificationService)
                .create(anyLong(), any(), anyString(), anyString(), any());

        assertDoesNotThrow(() -> transactionService.createDuplicateFraudAlert(sender, recipient,
                recipientWallet, money("500"), "remark"));
    }

    @Test void dailyLimitAlert_repositoryFailureIsSwallowed() {
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        when(transactionRepository.save(any(Transaction.class))).thenThrow(new RuntimeException("db down"));

        assertDoesNotThrow(() -> transactionService.createDailyLimitFraudAlert(sender, recipientWallet,
                money("600"), money("49900"), money("50500"), "remark"));
    }

    @Test void duplicateAlert_repositoryFailureIsSwallowed() {
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        when(transactionRepository.save(any(Transaction.class))).thenThrow(new RuntimeException("db down"));

        assertDoesNotThrow(() -> transactionService.createDuplicateFraudAlert(sender, recipient,
                recipientWallet, money("500"), "remark"));
    }
}
