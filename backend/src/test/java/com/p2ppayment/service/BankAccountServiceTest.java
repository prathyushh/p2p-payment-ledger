package com.p2ppayment.service;

import com.p2ppayment.entity.SimulatedAccountTransaction;
import com.p2ppayment.entity.SimulatedBankAccount;
import com.p2ppayment.entity.User;
import com.p2ppayment.repository.SimulatedAccountTransactionRepository;
import com.p2ppayment.repository.SimulatedBankAccountRepository;
import com.p2ppayment.repository.UserRepository;
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
class BankAccountServiceTest {

    @Mock
    private SimulatedBankAccountRepository bankAccountRepository;

    @Mock
    private SimulatedAccountTransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BankAccountService bankAccountService;

    private User testUser;
    private SimulatedBankAccount testAccount;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testAccount = new SimulatedBankAccount();
        testAccount.setId(1L);
        testAccount.setUser(testUser);
        testAccount.setBankName("Test Bank");
        testAccount.setAccountNickname("Test Account");
        testAccount.setAccountIdentifier("DEMO-1234567890");
        testAccount.setSimulatedBalance(new BigDecimal("5000.00"));
        testAccount.setIsActive(true);
    }

    @Test
    void createBankAccount_Success() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));

        when(bankAccountRepository.save(any(SimulatedBankAccount.class)))
                .thenAnswer(invocation -> {
                    SimulatedBankAccount account = invocation.getArgument(0);
                    account.setId(1L);
                    return account;
                });

        SimulatedBankAccount result = bankAccountService.createBankAccount(
                1L, "Test Bank", "Test Account"
        );

        assertNotNull(result);
        assertEquals("Test Bank", result.getBankName());
        assertEquals("Test Account", result.getAccountNickname());
        assertEquals(BigDecimal.ZERO, result.getSimulatedBalance());
        assertTrue(result.getIsActive());
        assertNotNull(result.getAccountIdentifier());
        assertTrue(result.getAccountIdentifier().startsWith("DEMO-"));

        verify(bankAccountRepository).save(any(SimulatedBankAccount.class));
    }

    @Test
    void createBankAccount_UserNotFound_ThrowsNotFound() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bankAccountService.createBankAccount(
                        1L, "Test Bank", "Test Account"
                )
        );

        assertEquals(404, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("User not found"));

        verify(bankAccountRepository, never())
                .save(any(SimulatedBankAccount.class));
    }

    @Test
    void listBankAccounts_Success() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));
        when(bankAccountRepository.findByUser(testUser))
                .thenReturn(List.of(testAccount));

        List<SimulatedBankAccount> result =
                bankAccountService.listBankAccounts(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("Test Account", result.get(0).getAccountNickname());
    }

    @Test
    void listBankAccounts_UserNotFound_ThrowsNotFound() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bankAccountService.listBankAccounts(1L)
        );

        assertEquals(404, exception.getStatusCode().value());

        verify(bankAccountRepository, never()).findByUser(any(User.class));
    }

    @Test
    void getBankAccount_Success() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));
        when(bankAccountRepository.findByIdAndUser(1L, testUser))
                .thenReturn(Optional.of(testAccount));

        SimulatedBankAccount result =
                bankAccountService.getBankAccount(1L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Account", result.getAccountNickname());
    }

    @Test
    void getBankAccount_AccountNotOwnedOrMissing_ThrowsNotFound() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));
        when(bankAccountRepository.findByIdAndUser(2L, testUser))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bankAccountService.getBankAccount(2L, 1L)
        );

        assertEquals(404, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("access denied"));
    }

    @Test
    void deactivateBankAccount_Success() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));
        when(bankAccountRepository.findByIdAndUser(1L, testUser))
                .thenReturn(Optional.of(testAccount));
        when(bankAccountRepository.save(any(SimulatedBankAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SimulatedBankAccount result =
                bankAccountService.deactivateBankAccount(1L, 1L);

        assertNotNull(result);
        assertFalse(result.getIsActive());

        verify(bankAccountRepository).save(testAccount);
    }

    @Test
    void deactivateBankAccount_AlreadyInactive_ThrowsConflict() {
        testAccount.setIsActive(false);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));
        when(bankAccountRepository.findByIdAndUser(1L, testUser))
                .thenReturn(Optional.of(testAccount));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bankAccountService.deactivateBankAccount(1L, 1L)
        );

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("already inactive"));

        verify(bankAccountRepository, never())
                .save(any(SimulatedBankAccount.class));
    }

    @Test
    void addDemoFunds_Success() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));
        when(bankAccountRepository.findByIdAndUser(1L, testUser))
                .thenReturn(Optional.of(testAccount));
        when(bankAccountRepository.save(any(SimulatedBankAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(SimulatedAccountTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SimulatedBankAccount result = bankAccountService.addDemoFunds(
                1L, 1L, new BigDecimal("1000.00")
        );

        assertNotNull(result);
        assertEquals(
                0,
                new BigDecimal("6000.00")
                        .compareTo(result.getSimulatedBalance())
        );

        verify(bankAccountRepository).save(testAccount);
        verify(transactionRepository).save(
                argThat(transaction ->
                        "DEMO_FUND_ADDED".equals(transaction.getOperationType())
                                && "CREDIT".equals(transaction.getDirection())
                                && "SUCCESS".equals(transaction.getOutcome())
                )
        );
    }

    @Test
    void addDemoFunds_ZeroAmount_ThrowsBadRequest() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));
        when(bankAccountRepository.findByIdAndUser(1L, testUser))
                .thenReturn(Optional.of(testAccount));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bankAccountService.addDemoFunds(
                        1L, 1L, BigDecimal.ZERO
                )
        );

        assertEquals(400, exception.getStatusCode().value());
        assertTrue(exception.getReason()
                .contains("Amount must be greater than zero"));

        verify(bankAccountRepository, never())
                .save(any(SimulatedBankAccount.class));
        verify(transactionRepository, never())
                .save(any(SimulatedAccountTransaction.class));
    }

    @Test
    void addDemoFunds_NegativeAmount_ThrowsBadRequest() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));
        when(bankAccountRepository.findByIdAndUser(1L, testUser))
                .thenReturn(Optional.of(testAccount));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bankAccountService.addDemoFunds(
                        1L, 1L, new BigDecimal("-100.00")
                )
        );

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void addDemoFunds_InactiveAccount_ThrowsConflict() {
        testAccount.setIsActive(false);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));
        when(bankAccountRepository.findByIdAndUser(1L, testUser))
                .thenReturn(Optional.of(testAccount));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bankAccountService.addDemoFunds(
                        1L, 1L, new BigDecimal("1000.00")
                )
        );

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("inactive"));

        verify(bankAccountRepository, never())
                .save(any(SimulatedBankAccount.class));
        verify(transactionRepository, never())
                .save(any(SimulatedAccountTransaction.class));
    }
}