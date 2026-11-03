package com.p2ppayment.service;

import com.p2ppayment.model.LoginAttempt;
import com.p2ppayment.model.Notification;
import com.p2ppayment.model.User;
import com.p2ppayment.repository.LoginAttemptRepository;
import com.p2ppayment.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LoginAttemptService loginAttemptService;

    private User testUser;
    private LoginAttempt testLoginAttempt;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setAccountLocked(false);
        testUser.setFailedLoginAttempts(0);

        testLoginAttempt = new LoginAttempt();
        testLoginAttempt.setId(1L);
        testLoginAttempt.setUser(testUser);
        testLoginAttempt.setSuccess(false);
        testLoginAttempt.setAttemptedAt(LocalDateTime.now());

        // Set default values for @Value fields
        ReflectionTestUtils.setField(loginAttemptService, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(loginAttemptService, "lockoutMinutes", 30);
    }

    @Test
    void recordSuccessfulAttempt_ResetsFailedAttempts() {
        // Arrange
        testUser.setFailedLoginAttempts(3);
        doNothing().when(userRepository).resetFailedLoginAttempts(anyLong());
        when(loginAttemptRepository.save(any(LoginAttempt.class))).thenReturn(testLoginAttempt);

        // Act
        loginAttemptService.recordSuccessfulAttempt(testUser, "127.0.0.1");

        // Assert
        verify(userRepository, times(1)).resetFailedLoginAttempts(eq(1L));
        verify(loginAttemptRepository, times(1)).save(any(LoginAttempt.class));
    }

    @Test
    void recordFailedAttempt_IncrementsFailedAttempts() {
        // Arrange
        testUser.setFailedLoginAttempts(2);
        doNothing().when(userRepository).incrementFailedLoginAttempts(anyLong());
        when(userRepository.getFailedLoginAttempts(anyLong())).thenReturn(3);
        when(loginAttemptRepository.save(any(LoginAttempt.class))).thenReturn(testLoginAttempt);

        // Act
        loginAttemptService.recordFailedAttempt(testUser, "127.0.0.1");

        // Assert
        verify(userRepository, times(1)).incrementFailedLoginAttempts(eq(1L));
        verify(userRepository, times(1)).getFailedLoginAttempts(eq(1L));
        verify(loginAttemptRepository, times(1)).save(any(LoginAttempt.class));
        verify(userRepository, never()).lockAccount(anyLong(), any(LocalDateTime.class));
    }

    @Test
    void recordFailedAttempt_FifthFailure_LocksAccount() {
        // Arrange
        testUser.setFailedLoginAttempts(4);
        doNothing().when(userRepository).incrementFailedLoginAttempts(anyLong());
        when(userRepository.getFailedLoginAttempts(anyLong())).thenReturn(5);
        doNothing().when(userRepository).lockAccount(anyLong(), any(LocalDateTime.class));
        when(loginAttemptRepository.save(any(LoginAttempt.class))).thenReturn(testLoginAttempt);
        doNothing().when(notificationService).create(anyLong(), any(Notification.Type.class), anyString(), anyString());

        // Act
        loginAttemptService.recordFailedAttempt(testUser, "127.0.0.1");

        // Assert
        verify(userRepository, times(1)).incrementFailedLoginAttempts(eq(1L));
        verify(userRepository, times(1)).getFailedLoginAttempts(eq(1L));
        verify(userRepository, times(1)).lockAccount(eq(1L), any(LocalDateTime.class));
        verify(loginAttemptRepository, times(1)).save(any(LoginAttempt.class));
        verify(notificationService, times(1)).create(eq(1L), eq(Notification.Type.ACCOUNT_LOCKED), anyString(), anyString());
    }

    @Test
    void unlockAccount_Success() {
        // Arrange
        testUser.setAccountLocked(true);
        testUser.setLockedUntil(LocalDateTime.now().plusMinutes(30));
        doNothing().when(userRepository).unlockAccount(anyLong());

        // Act
        loginAttemptService.unlockAccount(testUser);

        // Assert
        verify(userRepository, times(1)).unlockAccount(eq(1L));
    }

    @Test
    void recordFailedAttempt_NotificationFailure_DoesNotStopProcess() {
        // Arrange
        testUser.setFailedLoginAttempts(4);
        doNothing().when(userRepository).incrementFailedLoginAttempts(anyLong());
        when(userRepository.getFailedLoginAttempts(anyLong())).thenReturn(5);
        doNothing().when(userRepository).lockAccount(anyLong(), any(LocalDateTime.class));
        when(loginAttemptRepository.save(any(LoginAttempt.class))).thenReturn(testLoginAttempt);
        doThrow(new RuntimeException("Notification service down"))
            .when(notificationService).create(anyLong(), any(Notification.Type.class), anyString(), anyString());

        // Act - should not throw exception
        loginAttemptService.recordFailedAttempt(testUser, "127.0.0.1");

        // Assert - account should still be locked
        verify(userRepository, times(1)).lockAccount(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void recordSuccessfulAttempt_SavesAuditTrail() {
        // Arrange
        doNothing().when(userRepository).resetFailedLoginAttempts(anyLong());
        when(loginAttemptRepository.save(any(LoginAttempt.class))).thenReturn(testLoginAttempt);

        // Act
        loginAttemptService.recordSuccessfulAttempt(testUser, "192.168.1.1");

        // Assert
        verify(loginAttemptRepository, times(1)).save(argThat(attempt ->
            attempt.getUser().equals(testUser) &&
            attempt.getSuccess() &&
            "192.168.1.1".equals(attempt.getIpAddress())
        ));
    }
}
