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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPhoneNumber("1234567890");
        testUser.setPassword("hashedPassword");
        testUser.setTransactionPin("hashedPin");
        testUser.setRole(User.Role.USER);
        testUser.setEnabled(true);
        testUser.setAccountLocked(false);
        testUser.setFailedLoginAttempts(0);

        testWallet = new Wallet();
        testWallet.setId(1L);
        testWallet.setUser(testUser);
        testWallet.setBalance(BigDecimal.ZERO);
        testWallet.setDailySpent(BigDecimal.ZERO);
        testWallet.setStatus(Wallet.Status.ACTIVE);
    }

    private AuthRequest authRequest(String email, String password) {
        return new AuthRequest(email, password, "127.0.0.1");
    }

    private RegisterRequest registerRequest(String name, String email, String phone) {
        return new RegisterRequest(name, email, phone, "password123", "1234");
    }

    @Test
    void register_Success_NormalizesEmailAndCreatesWallet() {
        RegisterRequest request = registerRequest(
                "Test User", "  TEST@EXAMPLE.COM  ", "1234567890");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("1234567890")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-value");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateToken(1L, "test@example.com", "USER"))
                .thenReturn("test-token");

        RegisterResponse result = authService.register(request);

        assertNotNull(result);
        assertEquals("test-token", result.token());
        assertEquals(1L, result.id());
        assertEquals("Test User", result.name());
        assertEquals("test@example.com", result.email());
        assertEquals("USER", result.role());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("1234567890", savedUser.getPhoneNumber());
        assertEquals("encoded-value", savedUser.getPassword());
        assertEquals("encoded-value", savedUser.getTransactionPin());
        assertEquals(User.Role.USER, savedUser.getRole());
        assertTrue(savedUser.getEnabled());
        assertFalse(savedUser.getAccountLocked());
        assertEquals(0, savedUser.getFailedLoginAttempts());

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        Wallet savedWallet = walletCaptor.getValue();
        assertEquals(savedUser, savedWallet.getUser());
        assertEquals(0, BigDecimal.ZERO.compareTo(savedWallet.getBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(savedWallet.getDailySpent()));
        assertEquals(Wallet.Status.ACTIVE, savedWallet.getStatus());
    }

    @Test
    void register_EmailAlreadyExists_ThrowsConflict() {
        RegisterRequest request = registerRequest(
                "Test User", "test@example.com", "1234567890");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Email already registered", exception.getReason());
        verify(userRepository, never()).existsByPhoneNumber(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void register_PhoneAlreadyExists_ThrowsConflict() {
        RegisterRequest request = registerRequest(
                "Test User", "test@example.com", "1234567890");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("1234567890")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Phone number already registered", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void login_Success_NormalizesEmailAndRecordsSuccessfulAttempt() {
        AuthRequest request = authRequest("  TEST@EXAMPLE.COM ", "password123");
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateToken(1L, "test@example.com", "USER"))
                .thenReturn("test-token");

        AuthResponse result = authService.login(request);

        assertNotNull(result);
        assertEquals("test-token", result.token());
        assertEquals(1L, result.id());
        assertEquals("Test User", result.name());
        assertEquals("test@example.com", result.email());
        assertEquals("USER", result.role());

        verify(loginAttemptService).recordSuccessfulAttempt(testUser, "127.0.0.1");
        verify(loginAttemptService, never()).recordFailedAttempt(any(User.class), anyString());
        verify(jwtTokenProvider).generateToken(1L, "test@example.com", "USER");
    }

    @Test
    void login_UserNotFound_ThrowsUnauthorized() {
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(authRequest("missing@example.com", "password123")));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Invalid credentials", exception.getReason());
        verifyNoInteractions(passwordEncoder, loginAttemptService, jwtTokenProvider);
    }

    @Test
    void login_AccountLocked_ThrowsLocked() {
        testUser.setAccountLocked(true);
        testUser.setLockedUntil(null);
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(authRequest("test@example.com", "password123")));

        assertEquals(HttpStatus.LOCKED, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Account is locked"));
        verifyNoInteractions(passwordEncoder, loginAttemptService, jwtTokenProvider);
    }

    @Test
    void login_AccountDisabled_ThrowsForbidden() {
        testUser.setEnabled(false);
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(authRequest("test@example.com", "password123")));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Account is disabled", exception.getReason());
        verifyNoInteractions(passwordEncoder, loginAttemptService, jwtTokenProvider);
    }

    @Test
    void login_WrongPassword_RecordsFailureAndThrowsUnauthorized() {
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser), Optional.of(testUser));
        when(passwordEncoder.matches("wrong-password", "hashedPassword")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(authRequest("test@example.com", "wrong-password")));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Invalid credentials", exception.getReason());
        verify(loginAttemptService).recordFailedAttempt(testUser, "127.0.0.1");
        verify(loginAttemptService, never()).recordSuccessfulAttempt(any(User.class), anyString());
        verify(userRepository, times(2)).findByEmail("test@example.com");
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void login_WrongPasswordLocksAccount_ThrowsLocked() {
        User lockedUser = new User();
        lockedUser.setId(1L);
        lockedUser.setEmail("test@example.com");
        lockedUser.setAccountLocked(true);
        lockedUser.setLockedUntil(null);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser), Optional.of(lockedUser));
        when(passwordEncoder.matches("wrong-password", "hashedPassword")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(authRequest("test@example.com", "wrong-password")));

        assertEquals(HttpStatus.LOCKED, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Account is locked"));
        verify(loginAttemptService).recordFailedAttempt(testUser, "127.0.0.1");
        verify(loginAttemptService, never()).recordSuccessfulAttempt(any(User.class), anyString());
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void getCurrentUser_Success_ReturnsUserResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserResponse result = authService.getCurrentUser(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Test User", result.name());
        assertEquals("test@example.com", result.email());
        assertEquals("1234567890", result.phoneNumber());
        assertEquals("USER", result.role());
        verify(userRepository).findById(1L);
    }

    @Test
    void getCurrentUser_UserNotFound_ThrowsNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.getCurrentUser(999L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("User not found", exception.getReason());
        verify(userRepository).findById(999L);
    }
}
