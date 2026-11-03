package com.p2ppayment.controller;

import com.p2ppayment.dto.AdminUserResponse;
import com.p2ppayment.dto.TransactionResponse;
import com.p2ppayment.entity.Transaction;
import com.p2ppayment.entity.User;
import com.p2ppayment.entity.Wallet;
import com.p2ppayment.repository.UserRepository;
import com.p2ppayment.repository.WalletRepository;
import com.p2ppayment.repository.TransactionRepository;
import com.p2ppayment.service.AuditService;
import com.p2ppayment.service.WalletService;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private WalletRepository walletRepository;

    @MockBean
    private TransactionRepository transactionRepository;

    @MockBean
    private WalletService walletService;

    @MockBean
    private AuditService auditService;

    private User admin;
    private User targetUser;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "1", null, Collections.emptyList()));
        SecurityContextHolder.setContext(context);

        admin = mock(User.class);
        when(admin.getId()).thenReturn(1L);

        targetUser = mock(User.class);
        when(targetUser.getId()).thenReturn(2L);
        when(targetUser.getName()).thenReturn("Recipient User");
        when(targetUser.getEmail()).thenReturn("recipient@example.com");
        when(targetUser.getPhoneNumber()).thenReturn("9876543210");
        when(targetUser.getRole()).thenReturn(User.Role.USER);
        when(targetUser.getCreatedAt())
                .thenReturn(LocalDateTime.of(2025, 1, 1, 10, 0));
        when(targetUser.getAccountLocked()).thenReturn(false);
        when(targetUser.getLockedUntil()).thenReturn(null);

        wallet = mock(Wallet.class);
        when(wallet.getId()).thenReturn(10L);
        when(wallet.getBalance()).thenReturn(new BigDecimal("1000.00"));
        when(wallet.getStatus()).thenReturn(Wallet.Status.ACTIVE);
    }

    @org.junit.jupiter.api.AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listUsers_Success() throws Exception {
        when(userRepository.findAllUsers(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(targetUser)));

        when(walletRepository.findByUser(targetUser))
                .thenReturn(Optional.of(wallet));

        mockMvc.perform(get("/api/admin/users")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.content[0].name")
                        .value("Recipient User"))
                .andExpect(jsonPath("$.content[0].email")
                        .value("recipient@example.com"))
                .andExpect(jsonPath("$.content[0].phoneNumber")
                        .value("9876543210"))
                .andExpect(jsonPath("$.content[0].role").value("USER"))
                .andExpect(jsonPath("$.content[0].walletBalance")
                        .value(1000.00))
                .andExpect(jsonPath("$.content[0].walletStatus")
                        .value("ACTIVE"))
                .andExpect(jsonPath("$.content[0].accountLocked")
                        .value(false));

        verify(userRepository).findAllUsers(any(Pageable.class));
        verify(walletRepository).findByUser(targetUser);
    }

    @Test
    void listUsers_WithSearch_Success() throws Exception {
        when(userRepository.searchUsers(
                eq("recipient"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(targetUser)));

        when(walletRepository.findByUser(targetUser))
                .thenReturn(Optional.of(wallet));

        mockMvc.perform(get("/api/admin/users")
                        .param("search", "recipient"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.content[0].email")
                        .value("recipient@example.com"));

        verify(userRepository).searchUsers(
                eq("recipient"), any(Pageable.class));
        verify(userRepository, never()).findAllUsers(any(Pageable.class));
    }

    @Test
    void listUsers_EmptyList() throws Exception {
        when(userRepository.findAllUsers(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void getUser_Success() throws Exception {
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(targetUser));
        when(walletRepository.findByUser(targetUser))
                .thenReturn(Optional.of(wallet));

        mockMvc.perform(get("/api/admin/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Recipient User"))
                .andExpect(jsonPath("$.email")
                        .value("recipient@example.com"))
                .andExpect(jsonPath("$.walletBalance").value(1000.00));

        verify(userRepository).findById(2L);
    }

    @Test
    void getUser_NotFound() throws Exception {
        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserTransactions_Success() throws Exception {
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(targetUser));
        when(walletRepository.findByUser(targetUser))
                .thenReturn(Optional.of(wallet));

        Transaction transaction = createTransaction(wallet, wallet);

        when(transactionRepository.findBySenderWallet(
                eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(transaction)));

        mockMvc.perform(get("/api/admin/users/2/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.content[0].type")
                        .value("P2P_TRANSFER"))
                .andExpect(jsonPath("$.content[0].status")
                        .value("PENDING"))
                .andExpect(jsonPath("$.content[0].amount")
                        .value(250.00));

        verify(transactionRepository).findBySenderWallet(
                eq(10L), any(Pageable.class));
    }

    @Test
    void getUserTransactions_UserNotFound() throws Exception {
        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/users/99/transactions"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserTransactions_WalletNotFound() throws Exception {
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(targetUser));
        when(walletRepository.findByUser(targetUser))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/users/2/transactions"))
                .andExpect(status().isNotFound());
    }

    @Test
    void freezeWallet_Success() throws Exception {
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(targetUser));
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(admin));
        when(walletRepository.findByUser(targetUser))
                .thenReturn(Optional.of(wallet));

        mockMvc.perform(put("/api/admin/users/2/wallet/freeze")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isOk());

        verify(walletService).freezeWallet(
                eq(10L), eq(admin), eq("127.0.0.1"));
    }

    @Test
    void freezeWallet_UserNotFound() throws Exception {
        when(userRepository.findById(2L))
                .thenReturn(Optional.empty());

        mockMvc.perform(put("/api/admin/users/2/wallet/freeze"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(walletService);
    }

    @Test
    void freezeWallet_AdminNotFound() throws Exception {
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(targetUser));
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        mockMvc.perform(put("/api/admin/users/2/wallet/freeze"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(walletService);
    }

    @Test
    void freezeWallet_WalletNotFound() throws Exception {
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(targetUser));
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(admin));
        when(walletRepository.findByUser(targetUser))
                .thenReturn(Optional.empty());

        mockMvc.perform(put("/api/admin/users/2/wallet/freeze"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(walletService);
    }

    @Test
    void unfreezeWallet_Success() throws Exception {
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(targetUser));
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(admin));
        when(walletRepository.findByUser(targetUser))
                .thenReturn(Optional.of(wallet));

        mockMvc.perform(put("/api/admin/users/2/wallet/unfreeze")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isOk());

        verify(walletService).unfreezeWallet(
                eq(10L), eq(admin), eq("127.0.0.1"));
    }

    @Test
    void unfreezeWallet_UserNotFound() throws Exception {
        when(userRepository.findById(2L))
                .thenReturn(Optional.empty());

        mockMvc.perform(put("/api/admin/users/2/wallet/unfreeze"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(walletService);
    }

    @Test
    void unfreezeWallet_AdminNotFound() throws Exception {
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(targetUser));
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        mockMvc.perform(put("/api/admin/users/2/wallet/unfreeze"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(walletService);
    }

    @Test
    void unfreezeWallet_WalletNotFound() throws Exception {
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(targetUser));
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(admin));
        when(walletRepository.findByUser(targetUser))
                .thenReturn(Optional.empty());

        mockMvc.perform(put("/api/admin/users/2/wallet/unfreeze"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(walletService);
    }

    private Transaction createTransaction(Wallet senderWallet,
                                          Wallet receiverWallet) {
        Transaction transaction = mock(Transaction.class);

        User sender = mock(User.class);
        when(sender.getEmail()).thenReturn("sender@example.com");

        User receiver = mock(User.class);
        when(receiver.getEmail()).thenReturn("recipient@example.com");

        when(senderWallet.getUser()).thenReturn(sender);
        when(receiverWallet.getUser()).thenReturn(receiver);

        when(transaction.getId()).thenReturn(100L);
        when(transaction.getType())
                .thenReturn(Transaction.Type.P2P_TRANSFER);
        when(transaction.getStatus())
                .thenReturn(Transaction.Status.PENDING);
        when(transaction.getAmount())
                .thenReturn(new BigDecimal("250.00"));
        when(transaction.getSenderWallet()).thenReturn(senderWallet);
        when(transaction.getReceiverWallet()).thenReturn(receiverWallet);
        when(transaction.getRemark()).thenReturn("Test transfer");
        when(transaction.getSettlementDueDate())
                .thenReturn(LocalDate.of(2025, 1, 6));
        when(transaction.getInitiatedAt())
                .thenReturn(LocalDateTime.of(2025, 1, 3, 10, 0));
        when(transaction.getSettledAt()).thenReturn(null);

        return transaction;
    }
}