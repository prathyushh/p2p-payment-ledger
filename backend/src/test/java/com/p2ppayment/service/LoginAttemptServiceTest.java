package com.p2ppayment.service;

import com.p2ppayment.entity.LoginAttempt;
import com.p2ppayment.entity.Notification;
import com.p2ppayment.entity.User;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
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

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setAccountLocked(false);
        testUser.setFailedLoginAttempts(0);

        ReflectionTestUtils.setField(
                loginAttemptService, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(
                loginAttemptService, "lockoutMinutes", 30);
    }

    @Test
    void recordFailedAttempt_IncrementsFailedAttempts() {
        when(userRepository.getFailedLoginAttempts(1L)).thenReturn(3);

        loginAttemptService.recordFailedAttempt(testUser, "127.0.0.1");

        verify(userRepository).incrementFailedLoginAttempts(1L);
        verify(userRepository).getFailedLoginAttempts(1L);
        verify(loginAttemptRepository).save(argThat(attempt ->
                attempt.getUser().equals(testUser)
                        && Boolean.FALSE.equals(attempt.getSuccess())
                        && "127.0.0.1".equals(attempt.getIpAddress())
        ));
        verify(userRepository, never())
                .lockAccount(anyLong(), any(LocalDateTime.class));
    }

    @Test
    void recordFailedAttempt_FifthFailure_LocksAccount() {
        when(userRepository.getFailedLoginAttempts(1L)).thenReturn(5);

        loginAttemptService.recordFailedAttempt(testUser, "127.0.0.1");

        verify(userRepository).incrementFailedLoginAttempts(1L);
        verify(userRepository).getFailedLoginAttempts(1L);
        verify(userRepository).lockAccount(
                eq(1L), any(LocalDateTime.class));

        verify(loginAttemptRepository).save(any(LoginAttempt.class));
        verify(notificationService).create(
                eq(1L),
                eq(Notification.Type.ACCOUNT_LOCKED),
                eq("Account Locked"),
                anyString()
        );
    }

    @Test
    void unlockAccount_Success() {
        testUser.setAccountLocked(true);

        loginAttemptService.unlockAccount(testUser);

        verify(userRepository).unlockAccount(1L);
    }

    @Test
    void recordFailedAttempt_NotificationFailure_DoesNotStopProcess() {
        when(userRepository.getFailedLoginAttempts(1L)).thenReturn(5);

        doThrow(new RuntimeException("Notification service down"))
                .when(notificationService)
                .create(
                        anyLong(),
                        any(Notification.Type.class),
                        anyString(),
                        anyString()
                );

        assertDoesNotThrow(() ->
                loginAttemptService.recordFailedAttempt(
                        testUser, "127.0.0.1"));

        verify(userRepository).lockAccount(
                eq(1L), any(LocalDateTime.class));
        verify(loginAttemptRepository).save(any(LoginAttempt.class));
    }

    @Test
    void recordSuccessfulAttempt_ResetsCounterAndSavesAuditTrail() {
        loginAttemptService.recordSuccessfulAttempt(
                testUser, "192.168.1.1");

        verify(userRepository).resetFailedLoginAttempts(1L);

        verify(loginAttemptRepository).save(argThat(attempt ->
                attempt.getUser().equals(testUser)
                        && Boolean.TRUE.equals(attempt.getSuccess())
                        && "192.168.1.1".equals(attempt.getIpAddress())
        ));
    }
}