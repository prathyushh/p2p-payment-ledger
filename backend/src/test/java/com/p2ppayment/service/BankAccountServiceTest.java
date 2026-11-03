package com.p2ppayment.service;

import com.p2ppayment.model.SimulatedAccountTransaction;
import com.p2ppayment.model.SimulatedBankAccount;
import com.p2ppayment.model.User;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
        testAccount.setAccountNickname("Test Account");
        testAccount.setAccountIdentifier("ACC-12345");
        testAccount.setSimulatedBalance(new BigDecimal("5000.00"));
        testAccount.setIsActive(true);
    }

    @Test
    void createAccount_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bankAccountRepository.save(any(SimulatedBankAccount.class))).thenReturn(testAccount);

        // Act
        SimulatedBankAccount result = bankAccountService.createBankAccount(1L, "Test Bank", "Test Account");

        // Assert
        assertNotNull(result);
        assertEquals("Test Account", result.getAccountNickname());
        verify(bankAccountRepository, times(1)).save(any(SimulatedBankAccount.class));
    }

    @Test
    void createAccount_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            bankAccountService.createBankAccount(1L, "Test Bank", "Test Account");
        });

        assertEquals(404, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("User not found"));
    }

    @Test
    void getAccounts_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bankAccountRepository.findByUser(testUser))
            .thenReturn(Arrays.asList(testAccount));

        // Act
        List<SimulatedBankAccount> result = bankAccountService.listBankAccounts(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAccount.getId(), result.get(0).getId());
    }

    @Test
    void getAccount_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bankAccountRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testAccount));

        // Act
        SimulatedBankAccount result = bankAccountService.getBankAccount(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getAccount_Unauthorized_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bankAccountRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            bankAccountService.getBankAccount(1L, 1L);
        });

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void deactivateAccount_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bankAccountRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testAccount));
        when(bankAccountRepository.save(any(SimulatedBankAccount.class))).thenReturn(testAccount);

        // Act
        SimulatedBankAccount result = bankAccountService.deactivateBankAccount(1L, 1L);

        // Assert
        assertNotNull(result);
        assertFalse(result.getIsActive());
        verify(bankAccountRepository, times(1)).save(any(SimulatedBankAccount.class));
    }

    @Test
    void deactivateAccount_AlreadyInactive_ThrowsException() {
        // Arrange
        testAccount.setIsActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bankAccountRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testAccount));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            bankAccountService.deactivateBankAccount(1L, 1L);
        });

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("already inactive"));
    }

    @Test
    void addDemoFunds_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bankAccountRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testAccount));
        when(bankAccountRepository.save(any(SimulatedBankAccount.class))).thenReturn(testAccount);
        when(transactionRepository.save(any(SimulatedAccountTransaction.class))).thenReturn(null);

        // Act
        SimulatedBankAccount result = bankAccountService.addDemoFunds(1L, 1L, new BigDecimal("1000.00"));

        // Assert
        assertNotNull(result);
        verify(bankAccountRepository, times(1)).save(any(SimulatedBankAccount.class));
        verify(transactionRepository, times(1)).save(any(SimulatedAccountTransaction.class));
    }

    @Test
    void addDemoFunds_InvalidAmount_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bankAccountRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testAccount));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            bankAccountService.addDemoFunds(1L, 1L, BigDecimal.ZERO);
        });

        assertEquals(400, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Amount must be greater than zero"));
    }

    @Test
    void addDemoFunds_InactiveAccount_ThrowsException() {
        // Arrange
        testAccount.setIsActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bankAccountRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testAccount));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            bankAccountService.addDemoFunds(1L, 1L, new BigDecimal("1000.00"));
        });

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("inactive"));
    }
}
