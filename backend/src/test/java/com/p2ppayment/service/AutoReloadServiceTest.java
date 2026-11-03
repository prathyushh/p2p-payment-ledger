package com.p2ppayment.service;

import com.p2ppayment.model.*;
import com.p2ppayment.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoReloadServiceTest {

    @Mock
    private AutoReloadSettingsRepository autoReloadSettingsRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimulatedBankAccountRepository simulatedBankAccountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private SimulatedAccountTransactionRepository simulatedAccountTransactionRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AutoReloadService autoReloadService;

    private User testUser;
    private Wallet testWallet;
    private SimulatedBankAccount testBankAccount;
    private AutoReloadSettings testSettings;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testWallet = new Wallet();
        testWallet.setId(1L);
        testWallet.setUser(testUser);
        testWallet.setBalance(new BigDecimal("500.00"));
        testWallet.setStatus(Wallet.Status.ACTIVE);

        testBankAccount = new SimulatedBankAccount();
        testBankAccount.setId(1L);
        testBankAccount.setUser(testUser);
        testBankAccount.setSimulatedBalance(new BigDecimal("5000.00"));
        testBankAccount.setIsActive(true);

        testSettings = new AutoReloadSettings();
        testSettings.setId(1L);
        testSettings.setWallet(testWallet);
        testSettings.setSimulatedAccount(testBankAccount);
        testSettings.setIsEnabled(true);
        testSettings.setThresholdAmount(new BigDecimal("1000.00"));
        testSettings.setReloadAmount(new BigDecimal("2000.00"));
        testSettings.setReloadInProgress(false);
    }

    @Test
    void getSettings_ExistingSettings_ReturnsSettings() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(testWallet));
        when(autoReloadSettingsRepository.findByWallet(testWallet)).thenReturn(Optional.of(testSettings));

        // Act
        AutoReloadSettings result = autoReloadService.getSettings(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(testWallet.getId(), result.getWallet().getId());
    }

    @Test
    void getSettings_NoExistingSettings_CreatesNewSettings() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(testWallet));
        when(autoReloadSettingsRepository.findByWallet(testWallet)).thenReturn(Optional.empty());
        when(autoReloadSettingsRepository.save(any(AutoReloadSettings.class))).thenReturn(testSettings);

        // Act
        AutoReloadSettings result = autoReloadService.getSettings(1L);

        // Assert
        assertNotNull(result);
        verify(autoReloadSettingsRepository, times(1)).save(any(AutoReloadSettings.class));
    }

    @Test
    void updateSettings_EnableWithValidData_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(testWallet));
        when(simulatedBankAccountRepository.findById(1L)).thenReturn(Optional.of(testBankAccount));
        when(autoReloadSettingsRepository.findByWallet(testWallet)).thenReturn(Optional.of(testSettings));
        when(autoReloadSettingsRepository.save(any(AutoReloadSettings.class))).thenReturn(testSettings);

        // Act
        AutoReloadSettings result = autoReloadService.updateSettings(1L, true, 1L, 
            new BigDecimal("1000.00"), new BigDecimal("2000.00"));

        // Assert
        assertNotNull(result);
        assertTrue(result.getIsEnabled());
        verify(autoReloadSettingsRepository, times(1)).save(any(AutoReloadSettings.class));
    }

    @Test
    void updateSettings_EnableWithInactiveWallet_ThrowsException() {
        // Arrange
        testWallet.setStatus(Wallet.Status.INACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(testWallet));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            autoReloadService.updateSettings(1L, true, 1L, 
                new BigDecimal("1000.00"), new BigDecimal("2000.00"));
        });

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Wallet must be ACTIVE"));
    }

    @Test
    void updateSettings_EnableWithInactiveAccount_ThrowsException() {
        // Arrange
        testBankAccount.setIsActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(testWallet));
        when(simulatedBankAccountRepository.findById(1L)).thenReturn(Optional.of(testBankAccount));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            autoReloadService.updateSettings(1L, true, 1L, 
                new BigDecimal("1000.00"), new BigDecimal("2000.00"));
        });

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("must be active"));
    }

    @Test
    void updateSettings_EnableWithZeroThreshold_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(testWallet));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            autoReloadService.updateSettings(1L, true, 1L, 
                BigDecimal.ZERO, new BigDecimal("2000.00"));
        });

        assertEquals(400, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("thresholdAmount must be greater than 0"));
    }

    @Test
    void attemptReload_BalanceBelowThreshold_Success() {
        // Arrange
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(autoReloadSettingsRepository.findByWallet(testWallet)).thenReturn(Optional.of(testSettings));
        when(jdbcTemplate.update(anyString(), anyLong())).thenReturn(1); // Lock acquired
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(simulatedBankAccountRepository.save(any(SimulatedBankAccount.class))).thenReturn(testBankAccount);
        when(simulatedAccountTransactionRepository.save(any(SimulatedAccountTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(notificationService).create(anyLong(), any(), anyString(), anyString(), any());

        // Act
        autoReloadService.attemptReload(1L, false);

        // Assert
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(walletRepository, times(1)).save(any(Wallet.class));
        verify(ledgerEntryRepository, times(1)).save(any(LedgerEntry.class));
        verify(jdbcTemplate, times(2)).update(anyString(), anyLong()); // Acquire + release lock
    }

    @Test
    void attemptReload_BalanceAboveThreshold_NoReload() {
        // Arrange
        testWallet.setBalance(new BigDecimal("2000.00")); // Above threshold
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(autoReloadSettingsRepository.findByWallet(testWallet)).thenReturn(Optional.of(testSettings));

        // Act
        autoReloadService.attemptReload(1L, false);

        // Assert
        verify(jdbcTemplate, never()).update(anyString(), anyLong());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void attemptReload_SettingsDisabled_NoReload() {
        // Arrange
        testSettings.setIsEnabled(false);
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(autoReloadSettingsRepository.findByWallet(testWallet)).thenReturn(Optional.of(testSettings));

        // Act
        autoReloadService.attemptReload(1L, false);

        // Assert
        verify(jdbcTemplate, never()).update(anyString(), anyLong());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void attemptReload_InactiveWallet_NoReload() {
        // Arrange
        testWallet.setStatus(Wallet.Status.INACTIVE);
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(autoReloadSettingsRepository.findByWallet(testWallet)).thenReturn(Optional.of(testSettings));

        // Act
        autoReloadService.attemptReload(1L, false);

        // Assert
        verify(jdbcTemplate, never()).update(anyString(), anyLong());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void attemptReload_SkipAutoReloadCheck_NoReload() {
        // Arrange
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));

        // Act
        autoReloadService.attemptReload(1L, true); // Skip check

        // Assert
        verify(autoReloadSettingsRepository, never()).findByWallet(any());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void attemptReload_LockAlreadyAcquired_NoReload() {
        // Arrange
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(autoReloadSettingsRepository.findByWallet(testWallet)).thenReturn(Optional.of(testSettings));
        when(jdbcTemplate.update(anyString(), anyLong())).thenReturn(0); // Lock not acquired

        // Act
        autoReloadService.attemptReload(1L, false);

        // Assert
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(jdbcTemplate, times(1)).update(anyString(), anyLong()); // Only lock attempt
    }

    @Test
    void attemptReload_InsufficientBankBalance_CreatesFailedTransaction() {
        // Arrange
        testBankAccount.setSimulatedBalance(new BigDecimal("500.00")); // Insufficient
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(autoReloadSettingsRepository.findByWallet(testWallet)).thenReturn(Optional.of(testSettings));
        when(jdbcTemplate.update(anyString(), anyLong())).thenReturn(1); // Lock acquired
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(simulatedAccountTransactionRepository.save(any(SimulatedAccountTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(notificationService).create(anyLong(), any(), anyString(), anyString(), any());

        // Act
        autoReloadService.attemptReload(1L, false);

        // Assert
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(walletRepository, never()).save(any(Wallet.class)); // Wallet not updated on failure
        verify(jdbcTemplate, times(2)).update(anyString(), anyLong()); // Acquire + release lock
    }
}
