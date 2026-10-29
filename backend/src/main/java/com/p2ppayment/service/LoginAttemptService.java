package com.p2ppayment.service;

import com.p2ppayment.entity.LoginAttempt;
import com.p2ppayment.entity.Notification;
import com.p2ppayment.entity.User;
import com.p2ppayment.repository.LoginAttemptRepository;
import com.p2ppayment.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Value("${app.login-attempt.max-failures:5}")
    private int maxFailedAttempts;

    @Value("${app.login-attempt.lockout-minutes:30}")
    private int lockoutMinutes;

    public LoginAttemptService(LoginAttemptRepository loginAttemptRepository, 
                              UserRepository userRepository,
                              NotificationService notificationService) {
        this.loginAttemptRepository = loginAttemptRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void recordFailedAttempt(User user, String ipAddress) {
        // Record the failed attempt
        LoginAttempt attempt = LoginAttempt.builder()
                .user(user)
                .success(false)
                .ipAddress(ipAddress)
                .build();

        loginAttemptRepository.save(attempt);

        // Count failures in the last lockout period
        LocalDateTime since = LocalDateTime.now().minusMinutes(lockoutMinutes);
        long recentFailures = loginAttemptRepository.countRecentFailedAttempts(user, since);

        // Lock account if max failures reached
        if (recentFailures >= maxFailedAttempts) {
            user.setAccountLocked(true);
            user.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutMinutes));
            userRepository.save(user);
            
            // Create notification for account lock
            try {
                notificationService.create(
                    user.getId(),
                    Notification.Type.ACCOUNT_LOCKED,
                    "Account Locked",
                    "Your account has been locked due to " + maxFailedAttempts + " failed login attempts. It will be automatically unlocked in " + lockoutMinutes + " minutes."
                );
            } catch (Exception e) {
                // Log but do not fail the login attempt if notification creation fails
            }
        }
    }

    @Transactional
    public void recordSuccessfulAttempt(User user, String ipAddress) {
        // Record successful login
        LoginAttempt attempt = LoginAttempt.builder()
                .user(user)
                .success(true)
                .ipAddress(ipAddress)
                .build();

        loginAttemptRepository.save(attempt);

        // Reset lock if account was previously locked
        if (user.getAccountLocked() && user.getLockedUntil() != null) {
            if (LocalDateTime.now().isAfter(user.getLockedUntil())) {
                user.setAccountLocked(false);
                user.setLockedUntil(null);
                userRepository.save(user);
            }
        }
    }

    @Transactional(readOnly = true)
    public boolean isAccountLocked(User user) {
        if (!user.getAccountLocked()) {
            return false;
        }

        // If lockout has expired, account is no longer locked
        if (user.getLockedUntil() != null && LocalDateTime.now().isAfter(user.getLockedUntil())) {
            return false;
        }

        return true;
    }

    @Transactional
    public void unlockAccount(User user) {
        user.setAccountLocked(false);
        user.setLockedUntil(null);
        userRepository.save(user);
    }
}
