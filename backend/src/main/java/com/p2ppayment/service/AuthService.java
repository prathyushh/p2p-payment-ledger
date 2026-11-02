package com.p2ppayment.service;

import com.p2ppayment.dto.AuthRequest;
import com.p2ppayment.dto.AuthResponse;
import com.p2ppayment.dto.RegisterRequest;
import com.p2ppayment.dto.RegisterResponse;
import com.p2ppayment.dto.UserResponse;
import com.p2ppayment.entity.User;
import com.p2ppayment.entity.Wallet;
import com.p2ppayment.repository.UserRepository;
import com.p2ppayment.repository.WalletRepository;
import com.p2ppayment.util.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    public AuthService(UserRepository userRepository, WalletRepository walletRepository,
                       JwtTokenProvider jwtTokenProvider, PasswordEncoder passwordEncoder,
                       LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already registered");
        }

        User user = User.builder()
                .name(request.name())
                .email(normalizedEmail)
                .phoneNumber(request.phoneNumber())
                .password(passwordEncoder.encode(request.password()))
                .transactionPin(passwordEncoder.encode(request.transactionPin()))
                .role(User.Role.USER)
                .enabled(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .build();

        user = userRepository.save(user);

        walletRepository.save(Wallet.builder()
                .user(user)
                .balance(BigDecimal.ZERO)
                .dailySpent(BigDecimal.ZERO)
                .status(Wallet.Status.ACTIVE)
                .build());

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new RegisterResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    // NO @Transactional here - each DB call must commit independently
    public AuthResponse login(AuthRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        logger.info("[AUTH] Login attempt for: {}", normalizedEmail);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        // Check if account is locked.
        // ONLY the AccountAutoUnlockJob unlocks accounts (runs every 5 min).
        // Login never unlocks inline — user must wait for the job.
        if (Boolean.TRUE.equals(user.getAccountLocked())) {
            logger.warn("[AUTH] Account LOCKED for {} until {}", user.getEmail(), user.getLockedUntil());
            throw new ResponseStatusException(HttpStatus.LOCKED,
                    "Account is locked until " + user.getLockedUntil());
        }

        if (!user.getEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled");
        }

        // Check password
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            logger.warn("[AUTH] Wrong password for {}", user.getEmail());

            // This commits its own transaction independently
            loginAttemptService.recordFailedAttempt(user, request.ipAddress());

            // Reload fresh from DB to see if just locked
            user = userRepository.findByEmail(normalizedEmail).orElseThrow();
            if (Boolean.TRUE.equals(user.getAccountLocked())) {
                logger.warn("[AUTH] Account JUST LOCKED for {} until {}", user.getEmail(), user.getLockedUntil());
                throw new ResponseStatusException(HttpStatus.LOCKED,
                        "Account is locked until " + user.getLockedUntil());
            }

            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        logger.info("[AUTH] Successful login for {}", user.getEmail());
        loginAttemptService.recordSuccessfulAttempt(user, request.ipAddress());

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhoneNumber(), user.getRole().name());
    }
}

