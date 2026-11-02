package com.p2ppayment.service;

import com.p2ppayment.entity.LoginAttempt;
import com.p2ppayment.entity.Notification;
import com.p2ppayment.entity.User;
import com.p2ppayment.repository.LoginAttemptRepository;
import com.p2ppayment.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;

@Service
public class LoginAttemptService {

    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptService.class);

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

    @PostConstruct
    public void logConfiguration() {
        logger.info("=================================================================");
        logger.info("[LOGIN-SECURITY] Service Ready - Max Failures: {}, Lockout: {} min",
                    maxFailedAttempts, lockoutMinutes);
        logger.info("=================================================================");
    }

    @Transactional
    public void recordFailedAttempt(User user, String ipAddress) {
        // Step 1: Increment counter directly in DB (no JPA caching issues)
        userRepository.incrementFailedLoginAttempts(user.getId());

        // Step 2: Read the NEW count back from DB
        int newCount = userRepository.getFailedLoginAttempts(user.getId());

        logger.info("[LOGIN-SECURITY] User {} failure count: {}/{}", user.getEmail(), newCount, maxFailedAttempts);

        // Step 3: Save audit record
        loginAttemptRepository.save(LoginAttempt.builder()
                .user(user)
                .success(false)
                .ipAddress(ipAddress)
                .build());

        // Step 4: Lock if threshold reached
        if (newCount >= maxFailedAttempts) {
            LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(lockoutMinutes);
            userRepository.lockAccount(user.getId(), lockUntil);
            logger.warn("[LOGIN-SECURITY] *** ACCOUNT LOCKED *** {} until {}", user.getEmail(), lockUntil);

            try {
                notificationService.create(
                    user.getId(),
                    Notification.Type.ACCOUNT_LOCKED,
                    "Account Locked",
                    "Your account has been locked due to " + maxFailedAttempts +
                    " consecutive failed login attempts. It will automatically unlock in " +
                    lockoutMinutes + " minutes."
                );
            } catch (Exception e) {
                logger.error("[LOGIN-SECURITY] Could not send lock notification", e);
            }
        }
    }

    @Transactional
    public void recordSuccessfulAttempt(User user, String ipAddress) {
        // Reset counter directly in DB
        userRepository.resetFailedLoginAttempts(user.getId());

        logger.info("[LOGIN-SECURITY] Successful login - counter reset for {}", user.getEmail());

        loginAttemptRepository.save(LoginAttempt.builder()
                .user(user)
                .success(true)
                .ipAddress(ipAddress)
                .build());
    }

    @Transactional
    public void unlockAccount(User user) {
        userRepository.unlockAccount(user.getId());
        logger.info("[LOGIN-SECURITY] Account unlocked for {}", user.getEmail());
    }
}
