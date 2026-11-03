package com.p2ppayment.service;

import com.p2ppayment.model.*;
import com.p2ppayment.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private SimulatedBankAccountRepository simulatedBankAccountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private SimulatedAccountTransactionRepository simulatedAccountTransactionRepository;

    @Mock
    private AutoReloadService autoReloadService;

    @InjectMocks
    private WalletService walletService;

    private User testUser;
    private Wallet testWallet;
    private SimulatedBankAccount testBankAccount;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");

        testWallet = new Wallet();
        testWallet.setId(1L);
        testWallet.setUser(testUser);
        testWallet.setBalance(new BigDecimal("1000.00"));
        testWallet.setDailySpent(BigDecimal.ZERO);
        testWallet.setStatus(Wallet.Status.ACTIVE);

        testBankAccount = new SimulatedBankAccount();
        testBankAccount.setId(1L);
        testBankAccount.setUser(testUser);
        testBankAccount.setSimulatedBalance(new BigDecimal("5000.00"));
        testBankAccount.setIsActive(true);
        testBankAccount.setAccountNickname("Test Account");
    }

    @Test
    void getWalletByUserId_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(testWallet));

        // Act
        Wallet result = walletService.getWalletByUserId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(new BigDecimal("1000.00"), result.getBalance());
    }

    @Test
    void getWalletByUserId_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            walletService.getWalletByUserId(1L);
        });

        assertEquals(404, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("User not found"));
    }

    @Test
    void getWalletByUserId_WalletNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            walletService.getWalletByUserId(1L);
        });

        assertEquals(404, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Wallet not found"));
    }

    @Test
    void deactivateWallet_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
        doNothing().when(auditService).logWalletStatusChange(anyString(), anyLong(), anyString(), anyString(), any(User.class), anyString());

        // Act
        Wallet result = walletService.deactivateWallet(1L, "127.0.0.1");

        // Assert
        assertNotNull(result);
        assertEquals(Wallet.Status.INACTIVE, result.getStatus());
        verify(walletRepository, times(1)).save(any(Wallet.class));
        verify(auditService, times(1)).logWalletStatusChange(anyString(), anyLong(), anyString(), anyString(), any(User.class), anyString());
    }

    @Test
    void deactivateWallet_AlreadyInactive_ThrowsException() {
        // Arrange
        testWallet.setStatus(Wallet.Status.INACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(testWallet));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            walletService.deactivateWallet(1L, "127.0.0.1");
        });

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("already inactive"));
    }

    @Test
    void deactivateWallet_Frozen_ThrowsException() {
        // Arrange
        testWallet.setStatus(Wallet.Status.FROZEN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(testWallet));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            walletService.deactivateWallet(1L, "127.0.0.1");
        });

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Cannot deactivate a frozen wallet"));
    }

    @Test
    void reactivateWallet_Success() {
        // Arrange
        testWallet.setStatus(Wallet.Status.INACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
        doNothing().when(auditService).logWalletStatusChange(anyString(), anyLong(), anyString(), anyString(), any(User.class), anyString());

        // Act
        Wallet result = walletService.reactivateWallet(1L, "127.0.0.1");

        // Assert
        assertNotNull(result);
        assertEquals(Wallet.Status.ACTIVE, result.getStatus());
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    void reactivateWallet_AlreadyActive_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(testWallet));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            walletService.reactivateWallet(1L, "127.0.0.1");
        });

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("already active"));
    }

    @Test
    void freezeWallet_Success() {
        // Arrange
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
        doNothing().when(auditService).logWalletStatusChange(anyString(), anyLong(), anyString(), anyString(), any(User.class), anyString());

        // Act
        Wallet result = walletService.freezeWallet(1L, testUser, "127.0.0.1");

        // Assert
        assertNotNull(result);
        assertEquals(Wallet.Status.FROZEN, result.getStatus());
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    void unfreezeWallet_Success() {
        // Arrange
        testWallet.setStatus(Wallet.Status.FROZEN);
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
        doNothing().when(auditService).logWalletStatusChange(anyString(), anyLong(), anyString(), anyString(), any(User.class), anyString());

        // Act
        Wallet result = walletService.unfreezeWallet(1L, testUser, "127.0.0.1");

        // Assert
        assertNotNull(result);
        assertEquals(Wallet.Status.ACTIVE, result.getStatus());
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    void topUp_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(testWallet));
        when(simulatedBankAccountRepository.findById(1L)).thenReturn(Optional.of(testBankAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
        when(simulatedBankAccountRepository.save(any(SimulatedBankAccount.class))).thenReturn(testBankAccount);
        when(simulatedAccountTransactionRepository.save(any(SimulatedAccountTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(auditService).logWalletStatusChange(anyString(), anyLong(), anyString(), anyString(), any(User.class), any());
        doNothing().when(autoReloadService).attemptReload(anyLong(), anyBoolean());

        // Act
        Wallet result = walletService.topUp(1L, 1L, new BigDecimal("500.00"));

        // Assert
        assertNotNull(result);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(walletRepository, times(1)).save(any(Wallet.class));
        verify(ledgerEntryRepository, times(1)).save(any(LedgerEntry.class));
        verify(autoReloadService, times(1)).attemptReload(anyLong(), eq(false));
    }

    @Test
    void topUp_InvalidAmount_ThrowsException() {
        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            walletService.topUp(1L, 1L, BigDecimal.ZERO);
        });

        assertEquals(400, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Amount must be greater than 0"));
    }

    @Test
    void topUp_InactiveWallet_ThrowsException() {
        // Arrange
        testWallet.setStatus(Wallet.Status.INACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(testWallet));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            walletService.topUp(1L, 1L, new BigDecimal("500.00"));
        });

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Wallet is not active"));
    }

    @Test
    void topUp_InsufficientBankBalance_ThrowsException() {
        // Arrange
        testBankAccount.setSimulatedBalance(new BigDecimal("100.00"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(testWallet));
        when(simulatedBankAccountRepository.findById(1L)).thenReturn(Optional.of(testBankAccount));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            walletService.topUp(1L, 1L, new BigDecimal("500.00"));
        });

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Insufficient balance"));
    }

    @Test
    void topUp_InactiveBankAccount_ThrowsException() {
        // Arrange
        testBankAccount.setIsActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(testWallet));
        when(simulatedBankAccountRepository.findById(1L)).thenReturn(Optional.of(testBankAccount));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            walletService.topUp(1L, 1L, new BigDecimal("500.00"));
        });

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("inactive"));
    }

    @Test
    void topUp_UnauthorizedAccount_ThrowsException() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(2L);
        testBankAccount.setUser(otherUser);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(testWallet));
        when(simulatedBankAccountRepository.findById(1L)).thenReturn(Optional.of(testBankAccount));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            walletService.topUp(1L, 1L, new BigDecimal("500.00"));
        });

        assertEquals(403, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("do not have access"));
    }
}
