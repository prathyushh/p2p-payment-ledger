package com.p2ppayment.service;

import com.p2ppayment.model.Notification;
import com.p2ppayment.model.Transaction;
import com.p2ppayment.model.User;
import com.p2ppayment.repository.NotificationRepository;
import com.p2ppayment.repository.UserRepository;
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

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private Notification testNotification;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testTransaction = new Transaction();
        testTransaction.setId(1L);

        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setUser(testUser);
        testNotification.setType("TRANSFER_INITIATED");
        testNotification.setTitle("Transfer Initiated");
        testNotification.setMessage("Your transfer has been initiated");
        testNotification.setIsRead(false);
        testNotification.setRelatedTransaction(testTransaction);
    }

    @Test
    void create_WithTransaction_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // Act
        Notification result = notificationService.create(1L, Notification.Type.TRANSFER_INITIATED, 
            "Transfer Initiated", "Your transfer has been initiated", testTransaction);

        // Assert
        assertNotNull(result);
        assertEquals("Transfer Initiated", result.getTitle());
        assertFalse(result.getIsRead());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void create_WithoutTransaction_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // Act
        Notification result = notificationService.create(1L, Notification.Type.WALLET_ACTIVATED, 
            "Wallet Activated", "Your wallet is now active");

        // Assert
        assertNotNull(result);
        assertEquals("Transfer Initiated", result.getTitle());
        assertFalse(result.getIsRead());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void create_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            notificationService.create(1L, Notification.Type.TRANSFER_INITIATED, 
                "Transfer Initiated", "Your transfer has been initiated", testTransaction);
        });

        assertTrue(exception.getMessage().contains("User not found"));
    }

    @Test
    void getNotifications_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        Page<Notification> page = new PageImpl<>(Arrays.asList(testNotification));
        when(notificationRepository.findByUserId(1L, pageable)).thenReturn(page);

        // Act
        Page<Notification> result = notificationService.getNotifications(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testNotification.getId(), result.getContent().get(0).getId());
    }

    @Test
    void getUnreadCount_Success() {
        // Arrange
        when(notificationRepository.countUnreadByUserId(1L)).thenReturn(5L);

        // Act
        Long result = notificationService.getUnreadCount(1L);

        // Assert
        assertNotNull(result);
        assertEquals(5L, result);
    }

    @Test
    void markAsRead_Success() {
        // Arrange
        when(notificationRepository.markAsRead(1L, 1L)).thenReturn(1);

        // Act
        boolean result = notificationService.markAsRead(1L, 1L);

        // Assert
        assertTrue(result);
        verify(notificationRepository, times(1)).markAsRead(1L, 1L);
    }

    @Test
    void markAsRead_NotFound_ReturnsFalse() {
        // Arrange
        when(notificationRepository.markAsRead(1L, 1L)).thenReturn(0);

        // Act
        boolean result = notificationService.markAsRead(1L, 1L);

        // Assert
        assertFalse(result);
        verify(notificationRepository, times(1)).markAsRead(1L, 1L);
    }

    @Test
    void markAllAsRead_Success() {
        // Arrange
        when(notificationRepository.markAllAsRead(1L)).thenReturn(3);

        // Act
        int result = notificationService.markAllAsRead(1L);

        // Assert
        assertEquals(3, result);
        verify(notificationRepository, times(1)).markAllAsRead(1L);
    }

    @Test
    void markAllAsRead_NoUnread_ReturnsZero() {
        // Arrange
        when(notificationRepository.markAllAsRead(1L)).thenReturn(0);

        // Act
        int result = notificationService.markAllAsRead(1L);

        // Assert
        assertEquals(0, result);
        verify(notificationRepository, times(1)).markAllAsRead(1L);
    }
}
