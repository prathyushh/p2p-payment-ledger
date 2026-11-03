package com.p2ppayment.service;

import com.p2ppayment.model.*;
import com.p2ppayment.repository.FraudAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudServiceTest {

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @InjectMocks
    private FraudService fraudService;

    private Transaction testTransaction;
    private FraudAlert testAlert;

    @BeforeEach
    void setUp() {
        User sender = new User();
        sender.setId(1L);
        sender.setEmail("sender@example.com");

        User recipient = new User();
        recipient.setId(2L);
        recipient.setEmail("recipient@example.com");

        Wallet senderWallet = new Wallet();
        senderWallet.setId(1L);
        senderWallet.setUser(sender);

        Wallet recipientWallet = new Wallet();
        recipientWallet.setId(2L);
        recipientWallet.setUser(recipient);

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
        testAlert.setDescription("High value transfer");
    }

    @Test
    void createAlert_Success() {
        // Arrange
        when(fraudAlertRepository.save(any(FraudAlert.class))).thenReturn(testAlert);

        // Act
        FraudAlert result = fraudService.createAlert(testTransaction, 
            FraudAlert.AlertType.HIGH_VALUE, "High value transfer");

        // Assert
        assertNotNull(result);
        assertEquals(FraudAlert.AlertType.HIGH_VALUE, result.getAlertType());
        verify(fraudAlertRepository, times(1)).save(any(FraudAlert.class));
    }

    @Test
    void blockAlert_Success() {
        // Arrange
        when(fraudAlertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        when(fraudAlertRepository.save(any(FraudAlert.class))).thenReturn(testAlert);

        // Act
        FraudAlert result = fraudService.blockAlert(1L, "Suspicious activity confirmed");

        // Assert
        assertNotNull(result);
        assertEquals(FraudAlert.Status.BLOCKED, result.getStatus());
        assertEquals("BLOCKED", result.getAction());
        verify(fraudAlertRepository, times(1)).save(any(FraudAlert.class));
    }

    @Test
    void dismissAlert_Success() {
        // Arrange
        when(fraudAlertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        when(fraudAlertRepository.save(any(FraudAlert.class))).thenReturn(testAlert);

        // Act
        FraudAlert result = fraudService.dismissAlert(1L, "False positive");

        // Assert
        assertNotNull(result);
        assertEquals(FraudAlert.Status.DISMISSED, result.getStatus());
        assertEquals("DISMISSED", result.getAction());
        verify(fraudAlertRepository, times(1)).save(any(FraudAlert.class));
    }
}
