package com.p2ppayment.service;

import com.p2ppayment.entity.*;

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
           when(userRepository.findById(1L))
                   .thenReturn(Optional.of(testUser));
           when(walletRepository.findByUser(testUser))
                   .thenReturn(Optional.of(testWallet));
           when(autoReloadSettingsRepository.findByWallet(testWallet))
                   .thenReturn(Optional.of(testSettings));
           AutoReloadSettings result = autoReloadService.getSettings(1L);
           assertNotNull(result);
           assertEquals(1L, result.getId());
           assertEquals(testWallet.getId(), result.getWallet().getId());
           verify(autoReloadSettingsRepository, never())
                   .save(any(AutoReloadSettings.class));
       }
       @Test
       void getSettings_NoExistingSettings_CreatesSettings() {
           when(userRepository.findById(1L))
                   .thenReturn(Optional.of(testUser));
           when(walletRepository.findByUser(testUser))
                   .thenReturn(Optional.of(testWallet));
           when(autoReloadSettingsRepository.findByWallet(testWallet))
                   .thenReturn(Optional.empty());
           when(autoReloadSettingsRepository.save(any(AutoReloadSettings.class)))
                   .thenReturn(testSettings);
           AutoReloadSettings result = autoReloadService.getSettings(1L);
           assertNotNull(result);
           verify(autoReloadSettingsRepository)
                   .save(any(AutoReloadSettings.class));
       }
       @Test
       void getSettings_UserNotFound_ThrowsNotFound() {
           when(userRepository.findById(1L)).thenReturn(Optional.empty());
           ResponseStatusException exception = assertThrows(
                   ResponseStatusException.class,
                   () -> autoReloadService.getSettings(1L)
           );
           assertEquals(404, exception.getStatusCode().value());
       }
       @Test
       void getSettings_WalletNotFound_ThrowsNotFound() {
           when(userRepository.findById(1L))
                   .thenReturn(Optional.of(testUser));
           when(walletRepository.findByUser(testUser))
                   .thenReturn(Optional.empty());
           ResponseStatusException exception = assertThrows(
                   ResponseStatusException.class,
                   () -> autoReloadService.getSettings(1L)
           );
           assertEquals(404, exception.getStatusCode().value());
       }
       @Test
       void updateSettings_EnableWithValidData_Success() {
           when(userRepository.findById(1L))
                   .thenReturn(Optional.of(testUser));
           when(walletRepository.findByUser(testUser))
                   .thenReturn(Optional.of(testWallet));
           when(simulatedBankAccountRepository.findById(1L))
                   .thenReturn(Optional.of(testBankAccount));
           when(autoReloadSettingsRepository.findByWallet(testWallet))
                   .thenReturn(Optional.of(testSettings));
           when(autoReloadSettingsRepository.save(any(AutoReloadSettings.class)))
                   .thenAnswer(invocation -> invocation.getArgument(0));
           AutoReloadSettings result = autoReloadService.updateSettings(
                   1L,
                   true,
                   1L,
                   new BigDecimal("1000.00"),
                   new BigDecimal("2000.00")
           );
           assertNotNull(result);
           assertTrue(result.getIsEnabled());
           assertEquals(
                   0,
                   new BigDecimal("1000.00")
                           .compareTo(result.getThresholdAmount())
           );
           assertEquals(
                   0,
                   new BigDecimal("2000.00")
                           .compareTo(result.getReloadAmount())
           );
           verify(autoReloadSettingsRepository)
                   .save(any(AutoReloadSettings.class));
       }
       @Test
       void updateSettings_EnableWithInactiveWallet_ThrowsConflict() {
           testWallet.setStatus(Wallet.Status.INACTIVE);
           when(userRepository.findById(1L))
                   .thenReturn(Optional.of(testUser));
           when(walletRepository.findByUser(testUser))
                   .thenReturn(Optional.of(testWallet));
           ResponseStatusException exception = assertThrows(
                   ResponseStatusException.class,
                   () -> autoReloadService.updateSettings(
                           1L,
                           true,
                           1L,
                           new BigDecimal("1000.00"),
                           new BigDecimal("2000.00")
                   )
           );
           assertEquals(409, exception.getStatusCode().value());
           assertTrue(exception.getReason().contains("Wallet must be ACTIVE"));
       }
       @Test
       void updateSettings_EnableWithInactiveAccount_ThrowsConflict() {
           testBankAccount.setIsActive(false);
           when(userRepository.findById(1L))
                   .thenReturn(Optional.of(testUser));
           when(walletRepository.findByUser(testUser))
                   .thenReturn(Optional.of(testWallet));
           when(simulatedBankAccountRepository.findById(1L))
                   .thenReturn(Optional.of(testBankAccount));
           ResponseStatusException exception = assertThrows(
                   ResponseStatusException.class,
                   () -> autoReloadService.updateSettings(
                           1L,
                           true,
                           1L,
                           new BigDecimal("1000.00"),
                           new BigDecimal("2000.00")
                   )
           );
           assertEquals(409, exception.getStatusCode().value());
           assertTrue(exception.getReason().contains("must be active"));
       }
       @Test
       void updateSettings_EnableWithZeroThreshold_ThrowsBadRequest() {
           when(userRepository.findById(1L))
                   .thenReturn(Optional.of(testUser));
           when(walletRepository.findByUser(testUser))
                   .thenReturn(Optional.of(testWallet));
           ResponseStatusException exception = assertThrows(
                   ResponseStatusException.class,
                   () -> autoReloadService.updateSettings(
                           1L,
                           true,
                           1L,
                           BigDecimal.ZERO,
                           new BigDecimal("2000.00")
                   )
           );
           assertEquals(400, exception.getStatusCode().value());
           assertTrue(exception.getReason()
                   .contains("thresholdAmount must be greater than 0"));
       }
       @Test
       void updateSettings_EnableWithZeroReloadAmount_ThrowsBadRequest() {
           when(userRepository.findById(1L))
                   .thenReturn(Optional.of(testUser));
           when(walletRepository.findByUser(testUser))
                   .thenReturn(Optional.of(testWallet));
           ResponseStatusException exception = assertThrows(
                   ResponseStatusException.class,
                   () -> autoReloadService.updateSettings(
                           1L,
                           true,
                           1L,
                           new BigDecimal("1000.00"),
                           BigDecimal.ZERO
                   )
           );
           assertEquals(400, exception.getStatusCode().value());
           assertTrue(exception.getReason()
                   .contains("reloadAmount must be greater than 0"));
       }
       @Test
       void updateSettings_EnableWithMissingAccount_ThrowsNotFound() {
           when(userRepository.findById(1L))
                   .thenReturn(Optional.of(testUser));
           when(walletRepository.findByUser(testUser))
                   .thenReturn(Optional.of(testWallet));
           when(simulatedBankAccountRepository.findById(1L))
                   .thenReturn(Optional.empty());
           ResponseStatusException exception = assertThrows(
                   ResponseStatusException.class,
                   () -> autoReloadService.updateSettings(
                           1L,
                           true,
                           1L,
                           new BigDecimal("1000.00"),
                           new BigDecimal("2000.00")
                   )
           );
           assertEquals(404, exception.getStatusCode().value());
       }
       @Test
       void updateSettings_EnableWithAnotherUsersAccount_ThrowsForbidden() {
           User anotherUser = new User();
           anotherUser.setId(2L);
           anotherUser.setEmail("another@example.com");
           testBankAccount.setUser(anotherUser);
           when(userRepository.findById(1L))
                   .thenReturn(Optional.of(testUser));
           when(walletRepository.findByUser(testUser))
                   .thenReturn(Optional.of(testWallet));
           when(simulatedBankAccountRepository.findById(1L))
                   .thenReturn(Optional.of(testBankAccount));
           ResponseStatusException exception = assertThrows(
                   ResponseStatusException.class,
                   () -> autoReloadService.updateSettings(
                           1L,
                           true,
                           1L,
                           new BigDecimal("1000.00"),
                           new BigDecimal("2000.00")
                   )
           );
           assertEquals(403, exception.getStatusCode().value());
       }
       @Test
       void attemptReload_BalanceBelowThreshold_PerformsReload() {
           when(walletRepository.findById(1L))
                   .thenReturn(Optional.of(testWallet));
           when(autoReloadSettingsRepository.findByWallet(testWallet))
                   .thenReturn(Optional.of(testSettings));
           when(jdbcTemplate.update(anyString(), anyLong()))
                   .thenReturn(1);
           when(transactionRepository.save(any(Transaction.class)))
                   .thenAnswer(invocation -> invocation.getArgument(0));
           when(simulatedBankAccountRepository.save(any(SimulatedBankAccount.class)))
                   .thenAnswer(invocation -> invocation.getArgument(0));
           when(simulatedAccountTransactionRepository.save(
                   any(SimulatedAccountTransaction.class)))
                   .thenAnswer(invocation -> invocation.getArgument(0));
           when(walletRepository.save(any(Wallet.class)))
                   .thenAnswer(invocation -> invocation.getArgument(0));
           when(ledgerEntryRepository.save(any(LedgerEntry.class)))
                   .thenAnswer(invocation -> invocation.getArgument(0));
           autoReloadService.attemptReload(1L, false);
           assertEquals(
                   0,
                   new BigDecimal("2500.00").compareTo(testWallet.getBalance())
           );
           assertEquals(
                   0,
                   new BigDecimal("3000.00")
                           .compareTo(testBankAccount.getSimulatedBalance())
           );
           verify(transactionRepository).save(any(Transaction.class));
           verify(walletRepository).save(any(Wallet.class));
           verify(simulatedBankAccountRepository)
                   .save(any(SimulatedBankAccount.class));
           verify(simulatedAccountTransactionRepository)
                   .save(any(SimulatedAccountTransaction.class));
           verify(ledgerEntryRepository).save(any(LedgerEntry.class));
           // One update acquires the lock; the other releases it.
           verify(jdbcTemplate, times(2)).update(anyString(), anyLong());
       }
       @Test
       void attemptReload_BalanceAtOrAboveThreshold_DoesNotReload() {
           testWallet.setBalance(new BigDecimal("1000.00"));
           when(walletRepository.findById(1L))
                   .thenReturn(Optional.of(testWallet));
           when(autoReloadSettingsRepository.findByWallet(testWallet))
                   .thenReturn(Optional.of(testSettings));
           autoReloadService.attemptReload(1L, false);
           verify(jdbcTemplate, never()).update(anyString(), anyLong());
           verify(transactionRepository, never()).save(any(Transaction.class));
       }
       @Test
       void attemptReload_SettingsDisabled_DoesNotReload() {
           testSettings.setIsEnabled(false);
           when(walletRepository.findById(1L))
                   .thenReturn(Optional.of(testWallet));
           when(autoReloadSettingsRepository.findByWallet(testWallet))
                   .thenReturn(Optional.of(testSettings));
           autoReloadService.attemptReload(1L, false);
           verify(jdbcTemplate, never()).update(anyString(), anyLong());
           verify(transactionRepository, never()).save(any(Transaction.class));
       }
       @Test
       void attemptReload_InactiveWallet_DoesNotReload() {
           testWallet.setStatus(Wallet.Status.INACTIVE);
           when(walletRepository.findById(1L))
                   .thenReturn(Optional.of(testWallet));
           when(autoReloadSettingsRepository.findByWallet(testWallet))
                   .thenReturn(Optional.of(testSettings));
           autoReloadService.attemptReload(1L, false);
           verify(jdbcTemplate, never()).update(anyString(), anyLong());
           verify(transactionRepository, never()).save(any(Transaction.class));
       }
       @Test
       void attemptReload_SkipCheck_DoesNotLoadSettingsOrReload() {
           when(walletRepository.findById(1L))
                   .thenReturn(Optional.of(testWallet));
           autoReloadService.attemptReload(1L, true);
           verify(autoReloadSettingsRepository, never()).findByWallet(any());
           verify(jdbcTemplate, never()).update(anyString(), anyLong());
           verify(transactionRepository, never()).save(any(Transaction.class));
       }
       @Test
       void attemptReload_LockAlreadyAcquired_DoesNotReloadAgain() {
           when(walletRepository.findById(1L))
                   .thenReturn(Optional.of(testWallet));
           when(autoReloadSettingsRepository.findByWallet(testWallet))
                   .thenReturn(Optional.of(testSettings));
           when(jdbcTemplate.update(anyString(), anyLong()))
                   .thenReturn(0);
           autoReloadService.attemptReload(1L, false);
           verify(transactionRepository, never()).save(any(Transaction.class));
           verify(walletRepository, never()).save(any(Wallet.class));
           verify(jdbcTemplate, times(1)).update(anyString(), anyLong());
       }
       @Test
       void attemptReload_InsufficientBankBalance_RecordsFailedReload() {
           testBankAccount.setSimulatedBalance(new BigDecimal("500.00"));
           when(walletRepository.findById(1L))
                   .thenReturn(Optional.of(testWallet));
           when(autoReloadSettingsRepository.findByWallet(testWallet))
                   .thenReturn(Optional.of(testSettings));
           when(jdbcTemplate.update(anyString(), anyLong()))
                   .thenReturn(1);
           when(transactionRepository.save(any(Transaction.class)))
                   .thenAnswer(invocation -> invocation.getArgument(0));
           when(simulatedAccountTransactionRepository.save(
                   any(SimulatedAccountTransaction.class)))
                   .thenAnswer(invocation -> invocation.getArgument(0));
           autoReloadService.attemptReload(1L, false);
           // A failed reload is recorded, but wallet funds must not change.
           assertEquals(
                   0,
                   new BigDecimal("500.00").compareTo(testWallet.getBalance())
           );
           verify(transactionRepository).save(any(Transaction.class));
           verify(walletRepository, never()).save(any(Wallet.class));
           verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
           verify(jdbcTemplate, times(2)).update(anyString(), anyLong());
       }

    @Test
    void updateSettings_EnableWithMissingAccountId_ThrowsBadRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(testWallet));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> autoReloadService.updateSettings(
                        1L, true, null,
                        new BigDecimal("1000.00"),
                        new BigDecimal("2000.00")
                )
        );

        assertEquals(400, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("accountId is required"));
    }

    @Test
    void updateSettings_EnableWithMissingThreshold_ThrowsBadRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(testWallet));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> autoReloadService.updateSettings(
                        1L, true, 1L, null,
                        new BigDecimal("2000.00")
                )
        );

        assertEquals(400, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("thresholdAmount is required"));
    }

    @Test
    void updateSettings_EnableWithMissingReloadAmount_ThrowsBadRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(testWallet));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> autoReloadService.updateSettings(
                        1L, true, 1L,
                        new BigDecimal("1000.00"), null
                )
        );

        assertEquals(400, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("reloadAmount is required"));
    }

    @Test
    void updateSettings_DisableAutoReload_PreservesExistingValues() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(testWallet));
        when(autoReloadSettingsRepository.findByWallet(testWallet))
                .thenReturn(Optional.of(testSettings));
        when(autoReloadSettingsRepository.save(any(AutoReloadSettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AutoReloadSettings result = autoReloadService.updateSettings(
                1L, false, null, null, null
        );

        assertFalse(result.getIsEnabled());
        assertEquals(0, new BigDecimal("1000.00")
                .compareTo(result.getThresholdAmount()));
        assertEquals(0, new BigDecimal("2000.00")
                .compareTo(result.getReloadAmount()));
        assertSame(testBankAccount, result.getSimulatedAccount());

        verify(autoReloadSettingsRepository).save(testSettings);
    }

    @Test
    void attemptReload_WalletNotFound_DoesNothing() {
        when(walletRepository.findById(1L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> autoReloadService.attemptReload(1L, false));

        verify(autoReloadSettingsRepository, never()).findByWallet(any());
        verify(jdbcTemplate, never()).update(anyString(), anyLong());
    }

    @Test
    void attemptReload_SettingsNotFound_DoesNothing() {
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(autoReloadSettingsRepository.findByWallet(testWallet))
                .thenReturn(Optional.empty());

        autoReloadService.attemptReload(1L, false);

        verify(jdbcTemplate, never()).update(anyString(), anyLong());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void attemptReload_InactiveBankAccount_RecordsFailedReload() {
        testBankAccount.setIsActive(false);

        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(autoReloadSettingsRepository.findByWallet(testWallet))
                .thenReturn(Optional.of(testSettings));
        when(jdbcTemplate.update(anyString(), anyLong())).thenReturn(1);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(simulatedAccountTransactionRepository.save(
                any(SimulatedAccountTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        autoReloadService.attemptReload(1L, false);

        assertEquals(0, new BigDecimal("500.00")
                .compareTo(testWallet.getBalance()));
        assertEquals(0, new BigDecimal("5000.00")
                .compareTo(testBankAccount.getSimulatedBalance()));

        verify(transactionRepository).save(argThat(txn ->
                txn.getStatus() == Transaction.Status.FAILED));
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
        verify(notificationService).create(
                eq(1L),
                eq(Notification.Type.AUTO_RELOAD_FAILED),
                eq("Auto Reload Failed"),
                contains("inactive"),
                any(Transaction.class)
        );
        verify(jdbcTemplate, times(2)).update(anyString(), anyLong());
    }

    @Test
    void attemptReload_NullBankAccount_RecordsFailedReload() {
        testSettings.setSimulatedAccount(null);

        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(autoReloadSettingsRepository.findByWallet(testWallet))
                .thenReturn(Optional.of(testSettings));
        when(jdbcTemplate.update(anyString(), anyLong())).thenReturn(1);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        autoReloadService.attemptReload(1L, false);

        verify(transactionRepository).save(argThat(txn ->
                txn.getStatus() == Transaction.Status.FAILED));
        verify(simulatedAccountTransactionRepository, never())
                .save(any(SimulatedAccountTransaction.class));
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
        verify(jdbcTemplate, times(2)).update(anyString(), anyLong());
    }

}