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

import java.math.BigDecimal;

@Service
public class AuthService {

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
        // Normalize email: trim and lowercase
        String normalizedEmail = request.email().trim().toLowerCase();
        
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already registered");
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        String hashedPin = passwordEncoder.encode(request.transactionPin());

        User user = User.builder()
                .name(request.name())
                .email(normalizedEmail)
                .phoneNumber(request.phoneNumber())
                .password(hashedPassword)
                .transactionPin(hashedPin)
                .role(User.Role.USER)
                .enabled(true)
                .accountLocked(false)
                .build();

        user = userRepository.save(user);

        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(BigDecimal.ZERO)
                .dailySpent(BigDecimal.ZERO)
                .status(Wallet.Status.ACTIVE)
                .build();

        walletRepository.save(wallet);

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return new RegisterResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        // Normalize email: trim and lowercase
        String normalizedEmail = request.email().trim().toLowerCase();
        
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        // Check if account is locked (and handle auto-unlock if lockout period expired)
        if (loginAttemptService.isAccountLocked(user)) {
            // Attempt to auto-unlock if lockout period has expired
            if (user.getLockedUntil() != null && java.time.LocalDateTime.now().isAfter(user.getLockedUntil())) {
                loginAttemptService.unlockAccount(user);
            } else {
                throw new ResponseStatusException(HttpStatus.LOCKED, 
                    "Account is locked until " + user.getLockedUntil());
            }
        }

        if (!user.getEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled");
        }

        // Validate password
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            // Record failed attempt and potentially lock account
            loginAttemptService.recordFailedAttempt(user, request.ipAddress());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // Record successful attempt
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
