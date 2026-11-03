package com.p2ppayment.service;

import com.p2ppayment.entity.User;
import com.p2ppayment.repository.UserRepository;
import com.p2ppayment.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .phoneNumber("9876543210")
                .password("hashedPassword")
                .transactionPin("hashedPin")
                .role(User.Role.USER)
                .enabled(true)
                .accountLocked(false)
                .build();
    }

    @Test
    void loadUserByUsername_Success() {
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));

        UserDetails result =
                customUserDetailsService.loadUserByUsername("test@example.com");

        assertNotNull(result);
        assertInstanceOf(CustomUserDetails.class, result);
        assertEquals("test@example.com", result.getUsername());
        assertEquals("hashedPassword", result.getPassword());
        assertTrue(result.isEnabled());
        assertTrue(result.isAccountNonLocked());
        assertTrue(result.isAccountNonExpired());
        assertTrue(result.isCredentialsNonExpired());
        assertTrue(result.getAuthorities().contains(
                new SimpleGrantedAuthority("ROLE_USER")));

        verify(userRepository, times(1))
                .findByEmail("test@example.com");
    }

    @Test
    void loadUserByUsername_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername(
                        "missing@example.com")
        );

        assertTrue(exception.getMessage().contains(
                "User not found with email: missing@example.com"));

        verify(userRepository, times(1))
                .findByEmail("missing@example.com");
    }

    @Test
    void loadUserByUsername_AdminUser_ReturnsAdminAuthority() {
        testUser.setRole(User.Role.ADMIN);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));

        UserDetails result =
                customUserDetailsService.loadUserByUsername("test@example.com");

        assertNotNull(result);
        assertTrue(result.getAuthorities().contains(
                new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsername_DisabledUser_UserDetailsStillEnabled() {
        testUser.setEnabled(false);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));

        UserDetails result =
                customUserDetailsService.loadUserByUsername("test@example.com");

        assertNotNull(result);
        assertTrue(result.isEnabled());

        CustomUserDetails customDetails = (CustomUserDetails) result;
        assertFalse(customDetails.isUserEnabled());
    }

    @Test
    void loadUserByUsername_LockedUser_UserDetailsStillNonLocked() {
        testUser.setAccountLocked(true);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));

        UserDetails result =
                customUserDetailsService.loadUserByUsername("test@example.com");

        assertNotNull(result);
        assertTrue(result.isAccountNonLocked());

        CustomUserDetails customDetails = (CustomUserDetails) result;
        assertTrue(customDetails.isUserAccountLocked());
    }
}