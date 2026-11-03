package com.p2ppayment.service;

import com.p2ppayment.entity.*;
import com.p2ppayment.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    private User anotherUser;
    private Wallet testWallet;
    private SimulatedBankAccount testBankAccount;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);

        anotherUser = new User();
        anotherUser.setId(2L);

        testWallet = new Wallet();
        testWallet.setId(1L);
        testWallet.setUser(testUser);
        testWallet.setBalance(new BigDecimal("1000.00"));
        testWallet.setDailySpent(BigDecimal.ZERO);
        testWallet.setDailyLimit(new BigDecimal("50000.00"));
        testWallet.setStatus(Wallet.Status.ACTIVE);

        testBankAccount = new SimulatedBankAccount();
        testBankAccount.setId(1L);
        testBankAccount.setUser(testUser);
        testBankAccount.setBankName("Demo Bank");
        testBankAccount.setAccountNickname("Test Account");
        testBankAccount.setAccountIdentifier("TEST-ACCOUNT-001");
        testBankAccount.setSimulatedBalance(new BigDecimal("5000.00"));
        testBankAccount.setIsActive(true);
    }

    private void mockUserAndWallet() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));

        when(walletRepository.findByUser(testUser))
                .thenReturn(Optional.of(testWallet));
    }

    private void mockSuccessfulTopUpSaves() {
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction transaction = invocation.getArgument(0);
                    transaction.setId(100L);
                    return transaction;
                });

        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(simulatedBankAccountRepository.save(
                any(SimulatedBankAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(simulatedAccountTransactionRepository.save(
                any(SimulatedAccountTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(ledgerEntryRepository.save(any(LedgerEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // --------------------------------------------------
    // getWalletByUserId()
    // --------------------------------------------------

    @Test
    void getWalletByUserId_success() {
        mockUserAndWallet();

        Wallet result = walletService.getWalletByUserId(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(new BigDecimal("1000.00"), result.getBalance());
        assertEquals(Wallet.Status.ACTIVE, result.getStatus());
    }

    @Test
    void getWalletByUserId_userNotFound() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.getWalletByUserId(1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("User not found", exception.getReason());

        verifyNoInteractions(walletRepository);
    }

    @Test
    void getWalletByUserId_walletNotFound() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));

        when(walletRepository.findByUser(testUser))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.getWalletByUserId(1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Wallet not found", exception.getReason());
    }

    // --------------------------------------------------
    // deactivateWallet()
    // --------------------------------------------------

    @Test
    void deactivateWallet_success() {
        mockUserAndWallet();

        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Wallet result = walletService.deactivateWallet(1L, "127.0.0.1");

        assertEquals(Wallet.Status.INACTIVE, result.getStatus());

        verify(walletRepository).save(testWallet);
        verify(auditService).logWalletStatusChange(
                "WALLET_DEACTIVATE",
                1L,
                "ACTIVE",
                "INACTIVE",
                testUser,
                "127.0.0.1"
        );
    }

    @Test
    void deactivateWallet_alreadyInactive() {
        testWallet.setStatus(Wallet.Status.INACTIVE);
        mockUserAndWallet();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.deactivateWallet(1L, "127.0.0.1")
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Wallet is already inactive", exception.getReason());

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void deactivateWallet_frozenWallet() {
        testWallet.setStatus(Wallet.Status.FROZEN);
        mockUserAndWallet();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.deactivateWallet(1L, "127.0.0.1")
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Cannot deactivate a frozen wallet",
                exception.getReason()
        );

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    // --------------------------------------------------
    // reactivateWallet()
    // --------------------------------------------------

    @Test
    void reactivateWallet_success() {
        testWallet.setStatus(Wallet.Status.INACTIVE);
        mockUserAndWallet();

        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Wallet result = walletService.reactivateWallet(1L, "127.0.0.1");

        assertEquals(Wallet.Status.ACTIVE, result.getStatus());

        verify(auditService).logWalletStatusChange(
                "WALLET_REACTIVATE",
                1L,
                "INACTIVE",
                "ACTIVE",
                testUser,
                "127.0.0.1"
        );
    }

    @Test
    void reactivateWallet_alreadyActive() {
        mockUserAndWallet();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.reactivateWallet(1L, "127.0.0.1")
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Wallet is already active", exception.getReason());

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void reactivateWallet_frozenWallet() {
        testWallet.setStatus(Wallet.Status.FROZEN);
        mockUserAndWallet();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.reactivateWallet(1L, "127.0.0.1")
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Cannot reactivate a frozen wallet. Contact admin.",
                exception.getReason()
        );
    }

    // --------------------------------------------------
    // freezeWallet()
    // --------------------------------------------------

    @Test
    void freezeWallet_success() {
        when(walletRepository.findById(1L))
                .thenReturn(Optional.of(testWallet));

        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Wallet result = walletService.freezeWallet(
                1L, anotherUser, "127.0.0.1"
        );

        assertEquals(Wallet.Status.FROZEN, result.getStatus());

        verify(auditService).logWalletStatusChange(
                "WALLET_FREEZE",
                1L,
                "ACTIVE",
                "FROZEN",
                anotherUser,
                "127.0.0.1"
        );
    }

    @Test
    void freezeWallet_notFound() {
        when(walletRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.freezeWallet(
                        1L, anotherUser, "127.0.0.1"
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Wallet not found", exception.getReason());
    }

    @Test
    void freezeWallet_alreadyFrozen() {
        testWallet.setStatus(Wallet.Status.FROZEN);

        when(walletRepository.findById(1L))
                .thenReturn(Optional.of(testWallet));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.freezeWallet(
                        1L, anotherUser, "127.0.0.1"
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Wallet is already frozen", exception.getReason());

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    // --------------------------------------------------
    // unfreezeWallet()
    // --------------------------------------------------

    @Test
    void unfreezeWallet_success() {
        testWallet.setStatus(Wallet.Status.FROZEN);

        when(walletRepository.findById(1L))
                .thenReturn(Optional.of(testWallet));

        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Wallet result = walletService.unfreezeWallet(
                1L, anotherUser, "127.0.0.1"
        );

        assertEquals(Wallet.Status.ACTIVE, result.getStatus());

        verify(auditService).logWalletStatusChange(
                "WALLET_UNFREEZE",
                1L,
                "FROZEN",
                "ACTIVE",
                anotherUser,
                "127.0.0.1"
        );
    }

    @Test
    void unfreezeWallet_notFrozen() {
        when(walletRepository.findById(1L))
                .thenReturn(Optional.of(testWallet));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.unfreezeWallet(
                        1L, anotherUser, "127.0.0.1"
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Wallet is not frozen", exception.getReason());

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void unfreezeWallet_notFound() {
        when(walletRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.unfreezeWallet(
                        1L, anotherUser, "127.0.0.1"
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Wallet not found", exception.getReason());
    }

    // --------------------------------------------------
    // topUp()
    // --------------------------------------------------

    @Test
    void topUp_success() {
        mockUserAndWallet();

        when(simulatedBankAccountRepository.findById(1L))
                .thenReturn(Optional.of(testBankAccount));

        mockSuccessfulTopUpSaves();

        Wallet result = walletService.topUp(
                1L, 1L, new BigDecimal("500.00")
        );

        assertNotNull(result);
        assertEquals(new BigDecimal("1500.00"), result.getBalance());
        assertEquals(
                new BigDecimal("4500.00"),
                testBankAccount.getSimulatedBalance()
        );

        verify(transactionRepository).save(any(Transaction.class));
        verify(simulatedBankAccountRepository)
                .save(testBankAccount);
        verify(simulatedAccountTransactionRepository)
                .save(any(SimulatedAccountTransaction.class));
        verify(ledgerEntryRepository).save(any(LedgerEntry.class));
        verify(walletRepository).save(testWallet);

        verify(auditService).logWalletStatusChange(
                eq("WALLET_TOP_UP"),
                eq(1L),
                eq("1000.00"),
                eq("1500.00"),
                eq(testUser),
                isNull()
        );

        verify(autoReloadService).attemptReload(1L, false);
    }

    @Test
    void topUp_nullAmount() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.topUp(1L, 1L, null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Amount must be greater than 0", exception.getReason());

        verifyNoInteractions(userRepository);
    }

    @Test
    void topUp_zeroAmount() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.topUp(
                        1L, 1L, BigDecimal.ZERO
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Amount must be greater than 0", exception.getReason());
    }

    @Test
    void topUp_negativeAmount() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.topUp(
                        1L, 1L, new BigDecimal("-100.00")
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Amount must be greater than 0", exception.getReason());
    }

    @Test
    void topUp_userNotFound() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.topUp(
                        1L, 1L, new BigDecimal("500.00")
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("User not found", exception.getReason());
    }

    @Test
    void topUp_walletNotFound() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));

        when(walletRepository.findByUser(testUser))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.topUp(
                        1L, 1L, new BigDecimal("500.00")
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Wallet not found", exception.getReason());
    }

    @Test
    void topUp_walletInactive() {
        testWallet.setStatus(Wallet.Status.INACTIVE);
        mockUserAndWallet();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.topUp(
                        1L, 1L, new BigDecimal("500.00")
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Wallet is not active. Current status: INACTIVE",
                exception.getReason()
        );

        verify(simulatedBankAccountRepository, never()).findById(any());
    }

    @Test
    void topUp_walletFrozen() {
        testWallet.setStatus(Wallet.Status.FROZEN);
        mockUserAndWallet();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.topUp(
                        1L, 1L, new BigDecimal("500.00")
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Wallet is not active. Current status: FROZEN",
                exception.getReason()
        );
    }

    @Test
    void topUp_simulatedAccountNotFound() {
        mockUserAndWallet();

        when(simulatedBankAccountRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.topUp(
                        1L, 1L, new BigDecimal("500.00")
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Simulated account not found", exception.getReason());
    }

    @Test
    void topUp_accountBelongsToAnotherUser() {
        testBankAccount.setUser(anotherUser);

        mockUserAndWallet();

        when(simulatedBankAccountRepository.findById(1L))
                .thenReturn(Optional.of(testBankAccount));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.topUp(
                        1L, 1L, new BigDecimal("500.00")
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals(
                "You do not have access to this account",
                exception.getReason()
        );
    }

    @Test
    void topUp_simulatedAccountInactive() {
        testBankAccount.setIsActive(false);

        mockUserAndWallet();

        when(simulatedBankAccountRepository.findById(1L))
                .thenReturn(Optional.of(testBankAccount));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.topUp(
                        1L, 1L, new BigDecimal("500.00")
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Simulated account is inactive",
                exception.getReason()
        );
    }

    @Test
    void topUp_insufficientSimulatedAccountBalance() {
        testBankAccount.setSimulatedBalance(new BigDecimal("100.00"));

        mockUserAndWallet();

        when(simulatedBankAccountRepository.findById(1L))
                .thenReturn(Optional.of(testBankAccount));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> walletService.topUp(
                        1L, 1L, new BigDecimal("500.00")
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Insufficient balance in simulated account",
                exception.getReason()
        );
    }

    @Test
    void topUp_doesNotChangeBalancesWhenAccountBalanceIsInsufficient() {
        testBankAccount.setSimulatedBalance(new BigDecimal("100.00"));

        mockUserAndWallet();

        when(simulatedBankAccountRepository.findById(1L))
                .thenReturn(Optional.of(testBankAccount));

        assertThrows(
                ResponseStatusException.class,
                () -> walletService.topUp(
                        1L, 1L, new BigDecimal("500.00")
                )
        );

        assertEquals(
                new BigDecimal("1000.00"),
                testWallet.getBalance()
        );

        assertEquals(
                new BigDecimal("100.00"),
                testBankAccount.getSimulatedBalance()
        );

        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
        verify(autoReloadService, never()).attemptReload(any(), anyBoolean());
    }
}