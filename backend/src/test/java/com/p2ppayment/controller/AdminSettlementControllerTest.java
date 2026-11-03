
package com.p2ppayment.controller;

import com.p2ppayment.entity.Transaction;
import com.p2ppayment.entity.User;
import com.p2ppayment.entity.Wallet;
import com.p2ppayment.repository.TransactionRepository;
import com.p2ppayment.repository.UserRepository;
import com.p2ppayment.service.AuditService;
import com.p2ppayment.service.SettlementService;
import com.p2ppayment.security.JwtAuthenticationToken;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminSettlementController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminSettlementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionRepository transactionRepository;

    @MockBean
    private SettlementService settlementService;

    @MockBean
    private AuditService auditService;

    @MockBean
    private UserRepository userRepository;

    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        securityContext = SecurityContextHolder.createEmptyContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAdminAuthentication(String email) {
        JwtAuthenticationToken authentication =
                mock(JwtAuthenticationToken.class);

        when(authentication.getEmail()).thenReturn(email);
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private Transaction createTransaction(
            Long id,
            Transaction.Status status,
            LocalDate dueDate) {

        User sender = mock(User.class);
        when(sender.getEmail()).thenReturn("sender@example.com");

        User receiver = mock(User.class);
        when(receiver.getEmail()).thenReturn("receiver@example.com");

        Wallet senderWallet = mock(Wallet.class);
        when(senderWallet.getUser()).thenReturn(sender);

        Wallet receiverWallet = mock(Wallet.class);
        when(receiverWallet.getUser()).thenReturn(receiver);

        return Transaction.builder()
                .id(id)
                .type(Transaction.Type.P2P_TRANSFER)
                .status(status)
                .amount(new BigDecimal("100.00"))
                .senderWallet(senderWallet)
                .receiverWallet(receiverWallet)
                .remark("Test payment")
                .settlementDueDate(dueDate)
                .initiatedAt(LocalDateTime.now().minusDays(1))
                .settledAt(status == Transaction.Status.SUCCESS
                        ? LocalDateTime.now() : null)
                .build();
    }

    @Test
    void getSettlementQueue_Success() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        when(transactionRepository.countOverduePendingTransactions(today))
                .thenReturn(2L);
        when(transactionRepository.sumOverduePendingAmount(today))
                .thenReturn(new BigDecimal("2000.00"));
        when(transactionRepository.countPendingTransactionsDueDate(today))
                .thenReturn(3L);
        when(transactionRepository.sumPendingAmountDueDate(today))
                .thenReturn(new BigDecimal("3000.00"));
        when(transactionRepository.countPendingTransactionsDueDate(tomorrow))
                .thenReturn(4L);
        when(transactionRepository.countFuturePendingTransactions(today))
                .thenReturn(5L);
        when(transactionRepository.sumFuturePendingAmount(today))
                .thenReturn(new BigDecimal("5000.00"));

        mockMvc.perform(get("/api/admin/settlement/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overduePendingCount").value(2))
                .andExpect(jsonPath("$.overdueAmount").value(2000.00))
                .andExpect(jsonPath("$.dueTodayCount").value(3))
                .andExpect(jsonPath("$.dueTodayAmount").value(3000.00))
                .andExpect(jsonPath("$.dueTomorrowCount").value(4))
                .andExpect(jsonPath("$.totalPendingCount").value(10))
                .andExpect(jsonPath("$.totalPendingAmount").value(10000.00));
    }

    @Test
    void getSettlementQueue_NullAmountsUsesZeroDefaults()
            throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        when(transactionRepository.countOverduePendingTransactions(today))
                .thenReturn(0L);
        when(transactionRepository.sumOverduePendingAmount(today))
                .thenReturn(null);
        when(transactionRepository.countPendingTransactionsDueDate(today))
                .thenReturn(0L);
        when(transactionRepository.sumPendingAmountDueDate(today))
                .thenReturn(null);
        when(transactionRepository.countPendingTransactionsDueDate(tomorrow))
                .thenReturn(0L);
        when(transactionRepository.countFuturePendingTransactions(today))
                .thenReturn(0L);
        when(transactionRepository.sumFuturePendingAmount(today))
                .thenReturn(BigDecimal.ZERO);

        mockMvc.perform(get("/api/admin/settlement/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdueAmount").value(0))
                .andExpect(jsonPath("$.dueTodayAmount").value(0))
                .andExpect(jsonPath("$.dueTomorrowAmount").value(0))
                .andExpect(jsonPath("$.totalPendingAmount").value(0))
                .andExpect(jsonPath("$.totalPendingCount").value(0));
    }

    @Test
    void listPendingTransactions_NoTransactions() throws Exception {
        when(transactionRepository.findOverduePendingTransactions(any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(transactionRepository.findPendingTransactionsDueDate(any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(transactionRepository.findFuturePendingTransactions(any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/settlement/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdue_transactions").isEmpty())
                .andExpect(jsonPath("$.due_today_transactions").isEmpty())
                .andExpect(jsonPath("$.due_tomorrow_transactions").isEmpty())
                .andExpect(jsonPath("$.future_transactions").isEmpty())
                .andExpect(jsonPath("$.total_count").value(0));

        verify(transactionRepository).findOverduePendingTransactions(any(LocalDate.class));
        verify(transactionRepository, times(2))
                .findPendingTransactionsDueDate(any(LocalDate.class));
        verify(transactionRepository, never())
                .findFuturePendingTransactions(any(LocalDate.class));
    }

    @Test
    void listPendingTransactions_AllCategoriesIncluded() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        Transaction overdue = createTransaction(
                1L, Transaction.Status.PENDING, today.minusDays(1));
        Transaction dueToday = createTransaction(
                2L, Transaction.Status.PENDING, today);
        Transaction dueTomorrow = createTransaction(
                3L, Transaction.Status.PENDING, tomorrow);
        Transaction future = createTransaction(
                4L, Transaction.Status.PENDING, today.plusDays(5));

        when(transactionRepository.findOverduePendingTransactions(today))
                .thenReturn(Collections.singletonList(overdue));
        when(transactionRepository.findPendingTransactionsDueDate(today))
                .thenReturn(Collections.singletonList(dueToday));
        when(transactionRepository.findPendingTransactionsDueDate(tomorrow))
                .thenReturn(Collections.singletonList(dueTomorrow));
        when(transactionRepository.findFuturePendingTransactions(today))
                .thenReturn(Collections.singletonList(future));

        mockMvc.perform(get("/api/admin/settlement/pending")
                        .param("includeFuture", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdue_transactions.length()").value(1))
                .andExpect(jsonPath("$.due_today_transactions.length()").value(1))
                .andExpect(jsonPath("$.due_tomorrow_transactions.length()").value(1))
                .andExpect(jsonPath("$.future_transactions.length()").value(1))
                .andExpect(jsonPath("$.total_count").value(4))
                .andExpect(jsonPath("$.overdue_transactions[0].isOverdue").value(true))
                .andExpect(jsonPath("$.due_today_transactions[0].isOverdue").value(false))
                .andExpect(jsonPath("$.future_transactions[0].senderEmail")
                        .value("sender@example.com"));
    }

    @Test
    void listPendingTransactions_AllCategoriesExcluded() throws Exception {
        mockMvc.perform(get("/api/admin/settlement/pending")
                        .param("includeOverdue", "false")
                        .param("includeToday", "false")
                        .param("includeTomorrow", "false")
                        .param("includeFuture", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdue_transactions").isEmpty())
                .andExpect(jsonPath("$.due_today_transactions").isEmpty())
                .andExpect(jsonPath("$.due_tomorrow_transactions").isEmpty())
                .andExpect(jsonPath("$.future_transactions").isEmpty())
                .andExpect(jsonPath("$.total_count").value(0));

        verifyNoInteractions(transactionRepository);
    }

    @Test
    void triggerSettlement_TransactionNotFound() throws Exception {
        when(transactionRepository.findById(99L))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/admin/settlement/99/trigger"))
                .andExpect(status().isNotFound());

        verify(settlementService, never()).settleTransaction(any());
    }

    @Test
    void triggerSettlement_TransactionNotPending() throws Exception {
        Transaction transaction = createTransaction(
                1L, Transaction.Status.SUCCESS, LocalDate.now());

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(transaction));

        mockMvc.perform(post("/api/admin/settlement/1/trigger"))
                .andExpect(status().isConflict());

        verify(settlementService, never()).settleTransaction(any());
    }

    @Test
    void triggerSettlement_AdminNotFound() throws Exception {
        Transaction transaction = createTransaction(
                1L, Transaction.Status.PENDING, LocalDate.now());

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(transaction));
        setAdminAuthentication("admin@example.com");
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/admin/settlement/1/trigger"))
                .andExpect(status().isUnauthorized());

        verify(settlementService, never()).settleTransaction(any());
    }

    @Test
    void triggerSettlement_Success() throws Exception {
        Transaction transaction = createTransaction(
                1L, Transaction.Status.PENDING, LocalDate.now().minusDays(1));
        Transaction updatedTransaction = createTransaction(
                1L, Transaction.Status.SUCCESS, LocalDate.now().minusDays(1));
        User admin = mock(User.class);

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(transaction))
                .thenReturn(Optional.of(updatedTransaction));
        setAdminAuthentication("admin@example.com");
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(admin));

        mockMvc.perform(post("/api/admin/settlement/1/trigger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("P2P_TRANSFER"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.isOverdue").value(false));

        verify(auditService).logAction(
                eq("SETTLEMENT_TRIGGER"),
                eq("TRANSACTION"),
                eq(1L),
                eq(admin),
                contains("Manual settlement triggered by admin"),
                anyString());
        verify(settlementService).settleTransaction(transaction);
    }

    @Test
    void triggerSettlement_UpdatedTransactionMissing_Returns500()
            throws Exception {
        Transaction transaction = createTransaction(
                1L, Transaction.Status.PENDING, LocalDate.now());
        User admin = mock(User.class);

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(transaction))
                .thenReturn(Optional.empty());
        setAdminAuthentication("admin@example.com");
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(admin));

        mockMvc.perform(post("/api/admin/settlement/1/trigger"))
                .andExpect(status().isInternalServerError());

        verify(settlementService).settleTransaction(transaction);
    }

    @Test
    void triggerSettlement_ServiceFailure_Returns500() throws Exception {
        Transaction transaction = createTransaction(
                1L, Transaction.Status.PENDING, LocalDate.now());
        User admin = mock(User.class);

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(transaction));
        setAdminAuthentication("admin@example.com");
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(admin));
        doThrow(new RuntimeException("Settlement failed"))
                .when(settlementService).settleTransaction(transaction);

        mockMvc.perform(post("/api/admin/settlement/1/trigger"))
                .andExpect(status().isInternalServerError());

        verify(settlementService).settleTransaction(transaction);
    }
}