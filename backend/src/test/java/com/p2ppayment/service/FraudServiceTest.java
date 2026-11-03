package com.p2ppayment.service;

import com.p2ppayment.entity.*;
import com.p2ppayment.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudServiceTest {

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private FraudService fraudService;

    private Transaction testTransaction;
    private FraudAlert testAlert;
    private Wallet senderWallet;
    private Wallet recipientWallet;

    @BeforeEach
    void setUp() {
        User sender = new User();
        sender.setId(1L);
        sender.setName("Sender");
        sender.setEmail("sender@example.com");
        sender.setRole(User.Role.USER);

        User recipient = new User();
        recipient.setId(2L);
        recipient.setName("Recipient");
        recipient.setEmail("recipient@example.com");
        recipient.setRole(User.Role.USER);

        senderWallet = new Wallet();
        senderWallet.setId(1L);
        senderWallet.setUser(sender);
        senderWallet.setBalance(new BigDecimal("90000.00"));
        senderWallet.setStatus(Wallet.Status.ACTIVE);

        recipientWallet = new Wallet();
        recipientWallet.setId(2L);
        recipientWallet.setUser(recipient);
        recipientWallet.setBalance(new BigDecimal("1000.00"));
        recipientWallet.setStatus(Wallet.Status.ACTIVE);

        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setSenderWallet(senderWallet);
        testTransaction.setReceiverWallet(recipientWallet);
        testTransaction.setAmount(new BigDecimal("10000.00"));
        testTransaction.setType(Transaction.Type.P2P_TRANSFER);
        testTransaction.setStatus(Transaction.Status.PENDING);

        testAlert = new FraudAlert();
        testAlert.setId(1L);
        testAlert.setTransaction(testTransaction);
        testAlert.setAlertType(FraudAlert.AlertType.HIGH_VALUE);
        testAlert.setStatus(FraudAlert.Status.PENDING);
        testAlert.setNotes("High value transfer");
    }

    @Test
    void checkDailyLimit_WithinLimit_ReturnsTrue() {
        boolean result = fraudService.checkDailyLimit(
                1L,
                new BigDecimal("10000.00"),
                new BigDecimal("5000.00")
        );

        assertTrue(result);
    }

    @Test
    void checkDailyLimit_ExceedsLimit_ReturnsFalse() {
        boolean result = fraudService.checkDailyLimit(
                1L,
                new BigDecimal("45000.00"),
                new BigDecimal("10000.00")
        );

        assertFalse(result);
    }

    @Test
    void checkDuplicate_NoRecentDuplicates_ReturnsTrue() {
        when(transactionRepository.findRecentDuplicates(
                eq(1L), eq(2L), eq(new BigDecimal("10000.00")), any()
        )).thenReturn(List.of());

        boolean result = fraudService.checkDuplicate(
                1L, 2L, new BigDecimal("10000.00")
        );

        assertTrue(result);
    }

    @Test
    void checkDuplicate_RecentDuplicateExists_ReturnsFalse() {
        when(transactionRepository.findRecentDuplicates(
                eq(1L), eq(2L), eq(new BigDecimal("10000.00")), any()
        )).thenReturn(List.of(testTransaction));

        boolean result = fraudService.checkDuplicate(
                1L, 2L, new BigDecimal("10000.00")
        );

        assertFalse(result);
    }

    @Test
    void isHighValue_AtThreshold_ReturnsTrue() {
        assertTrue(fraudService.isHighValue(new BigDecimal("8000.00")));
    }

    @Test
    void isHighValue_BelowThreshold_ReturnsFalse() {
        assertFalse(fraudService.isHighValue(new BigDecimal("7999.99")));
    }

    @Test
    void createAlert_Success() {
        when(fraudAlertRepository.save(any(FraudAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FraudAlert result = fraudService.createAlert(
                testTransaction,
                FraudAlert.AlertType.HIGH_VALUE,
                "High value transfer"
        );

        assertNotNull(result);
        assertEquals(FraudAlert.AlertType.HIGH_VALUE, result.getAlertType());
        assertEquals(FraudAlert.Status.PENDING, result.getStatus());
        assertEquals("High value transfer", result.getNotes());
        assertSame(testTransaction, result.getTransaction());

        verify(fraudAlertRepository).save(any(FraudAlert.class));
    }

    @Test
    void blockAlert_AlertNotFound_ThrowsException() {
        when(fraudAlertRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> fraudService.blockAlert(1L, "Suspicious activity")
        );

        verify(fraudAlertRepository, never()).save(any(FraudAlert.class));
    }

    @Test
    void blockAlert_AlreadyBlocked_ThrowsException() {
        testAlert.setStatus(FraudAlert.Status.BLOCKED);

        when(fraudAlertRepository.findById(1L))
                .thenReturn(Optional.of(testAlert));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> fraudService.blockAlert(1L, "Suspicious activity")
        );

        assertEquals(409, exception.getStatusCode().value());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void dismissAlert_AlertNotFound_ThrowsException() {
        when(fraudAlertRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> fraudService.dismissAlert(1L, "False positive")
        );

        verify(fraudAlertRepository, never()).save(any(FraudAlert.class));
    }

    @Test
    void dismissAlert_Success() {
        when(fraudAlertRepository.findById(1L))
                .thenReturn(Optional.of(testAlert));

        when(fraudAlertRepository.save(any(FraudAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FraudAlert result = fraudService.dismissAlert(1L, "False positive");

        assertNotNull(result);
        assertEquals(FraudAlert.Status.DISMISSED, result.getStatus());
        assertEquals("False positive", result.getNotes());

        // Dismissing an alert should not change the transaction status.
        assertEquals(Transaction.Status.PENDING, testTransaction.getStatus());

        verify(fraudAlertRepository).save(testAlert);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}