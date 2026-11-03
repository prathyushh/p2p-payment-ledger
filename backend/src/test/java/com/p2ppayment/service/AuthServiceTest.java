package com.p2ppayment.service;

import com.p2ppayment.dto.AuthRequest;
import com.p2ppayment.dto.AuthResponse;
import com.p2ppayment.dto.RegisterRequest;
import com.p2ppayment.dto.RegisterResponse;
import com.p2ppayment.model.User;
import com.p2ppayment.model.Wallet;
import com.p2ppayment.repository.UserRepository;
import com.p2ppayment.repository.WalletRepository;
import com.p2ppayment.util.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Wallet testWallet;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setPhoneNumber("1234567890");
        testUser.setPassword("hashedPassword");
        testUser.setTransactionPin("hashedPin");
        testUser.setEnabled(true);
        testUser.setAccountLocked(false);
        testUser.setRole(User.Role.USER);

        testWallet = new Wallet();
        testWallet.setId(1L);
        testWallet.setUser(testUser);
        testWallet.setBalance(BigDecimal.ZERO);
        testWallet.setStatus(Wallet.Status.ACTIVE);
    }

    @Test
    void register_Success() {
        // Arrange
        RegisterRequest request = new RegisterRequest("Test User", "test@example.com", 
            "1234567890", "password123", "1234");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
        when(jwtTokenProvider.generateToken(anyLong(), anyString(), anyString())).thenReturn("test-token");

        // Act
        RegisterResponse result = authService.register(request);

        // Assert
        assertNotNull(result);
        assertEquals("test-token", result.token());
        verify(userRepository, times(1)).save(any(User.class));
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    void register_EmailAlreadyExists_ThrowsException() {
        // Arrange
        RegisterRequest request = new RegisterRequest("Test User", "test@example.com", 
            "1234567890", "password123", "1234");
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.register(request);
        });

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Email already registered"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_PhoneAlreadyExists_ThrowsException() {
        // Arrange
        RegisterRequest request = new RegisterRequest("Test User", "test@example.com", 
            "1234567890", "password123", "1234");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(true);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.register(request);
        });

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Phone number already registered"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        // Arrange
        AuthRequest request = new AuthRequest("test@example.com", "password123", "127.0.0.1");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenProvider.generateToken(anyLong(), anyString(), anyString())).thenReturn("test-token");
        doNothing().when(loginAttemptService).recordSuccessfulAttempt(any(User.class), anyString());

        // Act
        AuthResponse result = authService.login(request);

        // Assert
        assertNotNull(result);
        assertEquals("test-token", result.token());
        verify(loginAttemptService, times(1)).recordSuccessfulAttempt(any(User.class), anyString());
    }

    @Test
    void login_AccountLocked_ThrowsException() {
        // Arrange
        AuthRequest request = new AuthRequest("test@example.com", "password123", "127.0.0.1");
        testUser.setAccountLocked(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.login(request);
        });

        assertEquals(423, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("locked"));
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        // Arrange
        AuthRequest request = new AuthRequest("test@example.com", "password123", "127.0.0.1");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.login(request);
        });

        assertEquals(401, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Invalid credentials"));
    }

    @Test
    void login_AccountNotEnabled_ThrowsException() {
        // Arrange
        AuthRequest request = new AuthRequest("test@example.com", "password123", "127.0.0.1");
        testUser.setEnabled(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.login(request);
        });

        assertEquals(403, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("disabled"));
    }

    @Test
    void login_WrongPassword_ThrowsException() {
        // Arrange
        AuthRequest request = new AuthRequest("test@example.com", "wrongpassword", "127.0.0.1");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        doNothing().when(loginAttemptService).recordFailedAttempt(any(User.class), anyString());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.login(request);
        });

        assertEquals(401, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Invalid credentials"));
        verify(loginAttemptService, times(1)).recordFailedAttempt(any(User.class), anyString());
    }
}
