package com.p2ppayment.service;

import com.p2ppayment.model.*;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
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
        recipientWallet.setStatus(Wallet.Status.ACTIVE);

        transaction = new Transaction();
        transaction.setId(1L);
        transaction.setSenderWallet(senderWallet);
        transaction.setReceiverWallet(recipientWallet);
        transaction.setAmount(new BigDecimal("500.00"));
        transaction.setType(Transaction.Type.P2P_TRANSFER);
        transaction.setStatus(Transaction.Status.PENDING);
    }

    @Test
    void sendMoney_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(recipient));
        when(walletRepository.findByUser(recipient)).thenReturn(Optional.of(recipientWallet));
        when(transactionRepository.findRecentDuplicates(anyLong(), anyLong(), any(BigDecimal.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(walletRepository.save(any(Wallet.class))).thenReturn(senderWallet);
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(autoReloadService).attemptReload(anyLong(), anyBoolean());
        doNothing().when(fraudService).createAlert(any(), any(), anyString());
        doNothing().when(notificationService).create(anyLong(), any(), anyString(), anyString(), any());

        // Act
        Transaction result = transactionService.sendMoney(1L, "recipient@example.com", 
            new BigDecimal("500.00"), "Test payment", "1234");

        // Assert
        assertNotNull(result);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(walletRepository, atLeastOnce()).save(any(Wallet.class));
        verify(autoReloadService, times(1)).attemptReload(anyLong(), eq(false));
    }

    @Test
    void sendMoney_InvalidAmount_ThrowsException() {
        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            transactionService.sendMoney(1L, "recipient@example.com", BigDecimal.ZERO, "Test", "1234");
        });

        assertEquals(400, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Amount must be greater than 0"));
    }

    @Test
    void sendMoney_SenderNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            transactionService.sendMoney(1L, "recipient@example.com", 
                new BigDecimal("500.00"), "Test", "1234");
        });

        assertEquals(404, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Sender not found"));
    }

    @Test
    void sendMoney_InactiveWallet_ThrowsException() {
        // Arrange
        senderWallet.setStatus(Wallet.Status.INACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            transactionService.sendMoney(1L, "recipient@example.com", 
                new BigDecimal("500.00"), "Test", "1234");
        });

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("not active"));
    }

    @Test
    void sendMoney_InvalidPin_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            transactionService.sendMoney(1L, "recipient@example.com", 
                new BigDecimal("500.00"), "Test", "1234");
        });

        assertEquals(401, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Invalid PIN"));
    }

    @Test
    void sendMoney_RecipientNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            transactionService.sendMoney(1L, "nonexistent@example.com", 
                new BigDecimal("500.00"), "Test", "1234");
        });

        assertEquals(404, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Recipient not found"));
    }

    @Test
    void sendMoney_SelfTransfer_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sender));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            transactionService.sendMoney(1L, "sender@example.com", 
                new BigDecimal("500.00"), "Test", "1234");
        });

        assertEquals(400, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Cannot send money to yourself"));
    }

    @Test
    void sendMoney_RecipientWalletFrozen_ThrowsException() {
        // Arrange
        recipientWallet.setStatus(Wallet.Status.FROZEN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(recipient));
        when(walletRepository.findByUser(recipient)).thenReturn(Optional.of(recipientWallet));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            transactionService.sendMoney(1L, "recipient@example.com", 
                new BigDecimal("500.00"), "Test", "1234");
        });

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("frozen"));
    }

    @Test
    void sendMoney_InsufficientBalance_ThrowsException() {
        // Arrange
        senderWallet.setBalance(new BigDecimal("100.00"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(recipient));
        when(walletRepository.findByUser(recipient)).thenReturn(Optional.of(recipientWallet));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            transactionService.sendMoney(1L, "recipient@example.com", 
                new BigDecimal("500.00"), "Test", "1234");
        });

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Insufficient balance"));
    }

    @Test
    void sendMoney_DailyLimitExceeded_ThrowsException() {
        // Arrange
        senderWallet.setDailySpent(new BigDecimal("49000.00"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(recipient));
        when(walletRepository.findByUser(recipient)).thenReturn(Optional.of(recipientWallet));
        doNothing().when(fraudService).createAlert(any(), any(), anyString());
        when(userRepository.findAllAdminUsers()).thenReturn(List.of());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            transactionService.sendMoney(1L, "recipient@example.com", 
                new BigDecimal("2000.00"), "Test", "1234");
        });

        assertEquals(422, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Daily send limit"));
    }

    @Test
    void getTransactionHistory_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        Page<Transaction> transactionPage = new PageImpl<>(Arrays.asList(transaction), pageable, 1);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        when(transactionRepository.findByWallet(anyLong(), any(Pageable.class))).thenReturn(transactionPage);

        // Act
        Page<Transaction> result = transactionService.getTransactionHistory(1L, 0, 20);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(transaction.getId(), result.getContent().get(0).getId());
    }

    @Test
    void getMiniStatement_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        when(transactionRepository.findMiniStatement(anyLong(), any(Pageable.class)))
            .thenReturn(Arrays.asList(transaction));

        // Act
        List<Transaction> result = transactionService.getMiniStatement(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(transaction.getId(), result.get(0).getId());
    }

    @Test
    void getTransactionDetail_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        // Act
        Transaction result = transactionService.getTransactionDetail(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getTransactionDetail_Unauthorized_ThrowsException() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(3L);
        Wallet otherWallet = new Wallet();
        otherWallet.setId(3L);
        otherWallet.setUser(otherUser);

        when(userRepository.findById(3L)).thenReturn(Optional.of(otherUser));
        when(walletRepository.findByUser(otherUser)).thenReturn(Optional.of(otherWallet));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            transactionService.getTransactionDetail(1L, 3L);
        });

        assertEquals(403, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("not authorized"));
    }
}
