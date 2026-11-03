package com.p2ppayment.service;

import com.p2ppayment.entity.Notification;
import com.p2ppayment.entity.Transaction;
import com.p2ppayment.entity.User;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
        testNotification.setType(
                Notification.Type.TRANSFER_INITIATED.name());
        testNotification.setTitle("Transfer Initiated");
        testNotification.setMessage(
                "Your transfer has been initiated");
        testNotification.setIsRead(false);
        testNotification.setRelatedTransaction(testTransaction);
    }

    @Test
    void create_WithTransaction_Success() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationService.create(
                1L,
                Notification.Type.TRANSFER_INITIATED,
                "Transfer Initiated",
                "Your transfer has been initiated",
                testTransaction
        );

        assertNotNull(result);
        assertEquals("Transfer Initiated", result.getTitle());
        assertEquals(
                Notification.Type.TRANSFER_INITIATED.name(),
                result.getType());
        assertFalse(result.getIsRead());
        assertSame(testTransaction, result.getRelatedTransaction());

        verify(userRepository).findById(1L);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void create_WithoutTransaction_Success() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationService.create(
                1L,
                Notification.Type.WALLET_TOP_UP,
                "Wallet Activated",
                "Your wallet is now active"
        );

        assertNotNull(result);
        assertEquals("Wallet Activated", result.getTitle());
        assertEquals(
                Notification.Type.WALLET_TOP_UP.name(),
                result.getType());
        assertFalse(result.getIsRead());
        assertNull(result.getRelatedTransaction());

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void create_UserNotFound_ThrowsException() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.create(
                        1L,
                        Notification.Type.TRANSFER_INITIATED,
                        "Transfer Initiated",
                        "Your transfer has been initiated",
                        testTransaction
                )
        );

        assertTrue(exception.getMessage().contains("User not found"));

        verify(notificationRepository, never())
                .save(any(Notification.class));
    }

    @Test
    void getNotifications_Success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Notification> expectedPage =
                new PageImpl<>(List.of(testNotification));

        when(notificationRepository.findByUserId(1L, pageable))
                .thenReturn(expectedPage);

        Page<Notification> result =
                notificationService.getNotifications(1L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(
                testNotification.getId(),
                result.getContent().get(0).getId());

        verify(notificationRepository).findByUserId(1L, pageable);
    }

    @Test
    void getUnreadCount_Success() {
        when(notificationRepository.countUnreadByUserId(1L))
                .thenReturn(5L);

        Long result = notificationService.getUnreadCount(1L);

        assertEquals(5L, result);
        verify(notificationRepository).countUnreadByUserId(1L);
    }

    @Test
    void markAsRead_Success() {
        when(notificationRepository.markAsRead(1L, 1L))
                .thenReturn(1);

        boolean result = notificationService.markAsRead(1L, 1L);

        assertTrue(result);
        verify(notificationRepository).markAsRead(1L, 1L);
    }

    @Test
    void markAsRead_NotFound_ReturnsFalse() {
        when(notificationRepository.markAsRead(1L, 1L))
                .thenReturn(0);

        boolean result = notificationService.markAsRead(1L, 1L);

        assertFalse(result);
        verify(notificationRepository).markAsRead(1L, 1L);
    }

    @Test
    void markAllAsRead_Success() {
        when(notificationRepository.markAllAsRead(1L))
                .thenReturn(3);

        int result = notificationService.markAllAsRead(1L);

        assertEquals(3, result);
        verify(notificationRepository).markAllAsRead(1L);
    }
}