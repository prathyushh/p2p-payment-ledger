package com.p2ppayment.service;

import com.p2ppayment.entity.User;
import com.p2ppayment.entity.Wallet;
import com.p2ppayment.repository.UserRepository;
import com.p2ppayment.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminBootstrapService adminBootstrapService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                adminBootstrapService, "adminEmail", " ADMIN@EXAMPLE.COM ");
        ReflectionTestUtils.setField(
                adminBootstrapService, "adminPassword", "secure-admin-password");
    }

    @Test
    void bootstrapAdmin_CreatesAdminAndWallet_WhenAdminDoesNotExist() {
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode("secure-admin-password"))
                .thenReturn("hashed-admin-password");
        when(passwordEncoder.encode("1234")).thenReturn("hashed-pin");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });
        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        adminBootstrapService.bootstrapAdmin();

        verify(userRepository).findByEmail("admin@example.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedAdmin = userCaptor.getValue();

        assertEquals("Admin User", savedAdmin.getName());
        assertEquals("admin@example.com", savedAdmin.getEmail());
        assertEquals("0000000000", savedAdmin.getPhoneNumber());
        assertEquals("hashed-admin-password", savedAdmin.getPassword());
        assertEquals("hashed-pin", savedAdmin.getTransactionPin());
        assertEquals(User.Role.ADMIN, savedAdmin.getRole());
        assertTrue(savedAdmin.getEnabled());
        assertFalse(savedAdmin.getAccountLocked());

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        Wallet savedWallet = walletCaptor.getValue();

        assertEquals(savedAdmin, savedWallet.getUser());
        assertEquals(0, BigDecimal.ZERO.compareTo(savedWallet.getBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(savedWallet.getDailySpent()));
        assertEquals(Wallet.Status.ACTIVE, savedWallet.getStatus());

        verify(passwordEncoder).encode("secure-admin-password");
        verify(passwordEncoder).encode("1234");
    }

    @Test
    void bootstrapAdmin_ExistingAdmin_DoesNotCreateAnotherAdmin() {
        User existingAdmin = new User();
        existingAdmin.setId(1L);
        existingAdmin.setEmail("admin@example.com");
        existingAdmin.setRole(User.Role.ADMIN);

        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(existingAdmin));

        adminBootstrapService.bootstrapAdmin();

        verify(userRepository).findByEmail("admin@example.com");
        verify(userRepository, never()).save(any(User.class));
        verify(walletRepository, never()).save(any(Wallet.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void bootstrapAdmin_EmptyPassword_SkipsCreation() {
        ReflectionTestUtils.setField(adminBootstrapService, "adminPassword", "");

        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.empty());

        adminBootstrapService.bootstrapAdmin();

        verify(userRepository).findByEmail("admin@example.com");
        verify(userRepository, never()).save(any(User.class));
        verify(walletRepository, never()).save(any(Wallet.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void bootstrapAdmin_NullPassword_SkipsCreation() {
        ReflectionTestUtils.setField(adminBootstrapService, "adminPassword", null);

        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.empty());

        adminBootstrapService.bootstrapAdmin();

        verify(userRepository, never()).save(any(User.class));
        verify(walletRepository, never()).save(any(Wallet.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void bootstrapAdmin_DefaultPlaceholderPassword_SkipsCreation() {
        ReflectionTestUtils.setField(
                adminBootstrapService, "adminPassword", "admin@example.com");

        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.empty());

        adminBootstrapService.bootstrapAdmin();

        verify(userRepository, never()).save(any(User.class));
        verify(walletRepository, never()).save(any(Wallet.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void bootstrapAdmin_ExistingNonAdminUserWithSameEmail_CreatesAdmin() {
        User existingUser = new User();
        existingUser.setId(2L);
        existingUser.setEmail("admin@example.com");
        existingUser.setRole(User.Role.USER);

        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("secure-admin-password"))
                .thenReturn("hashed-admin-password");
        when(passwordEncoder.encode("1234")).thenReturn("hashed-pin");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });
        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        adminBootstrapService.bootstrapAdmin();

        verify(userRepository).save(any(User.class));
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void adminBootstrapRunner_ReturnsRunnerThatCallsBootstrapAdmin() throws Exception {
        AdminBootstrapService service = spy(adminBootstrapService);
        ApplicationRunner runner = service.adminBootstrapRunner();

        assertNotNull(runner);

        ApplicationArguments arguments = mock(ApplicationArguments.class);
        runner.run(arguments);

        verify(service).bootstrapAdmin();
    }
}
