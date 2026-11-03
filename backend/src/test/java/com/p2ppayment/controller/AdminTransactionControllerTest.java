package com.p2ppayment.controller;

import com.p2ppayment.entity.LedgerEntry;
import com.p2ppayment.entity.Transaction;
import com.p2ppayment.entity.User;
import com.p2ppayment.entity.Wallet;
import com.p2ppayment.repository.LedgerEntryRepository;
import com.p2ppayment.repository.TransactionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;

import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminTransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionRepository transactionRepository;

    @MockBean
    private LedgerEntryRepository ledgerEntryRepository;

    private Transaction transaction;

    private User sender;
    private User receiver;

    private Wallet senderWallet;
    private Wallet receiverWallet;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setId(1L);
        sender.setName("Sender User");
        sender.setEmail("sender@example.com");

        receiver = new User();
        receiver.setId(2L);
        receiver.setName("Receiver User");
        receiver.setEmail("receiver@example.com");

        senderWallet = new Wallet();
        senderWallet.setId(10L);
        senderWallet.setUser(sender);

        receiverWallet = new Wallet();
        receiverWallet.setId(20L);
        receiverWallet.setUser(receiver);

        transaction = createTransaction(1L);
    }

    private Transaction createTransaction(Long id) {
        return Transaction.builder()
                .id(id)
                .requestId("request-" + id)
                .type(Transaction.Type.P2P_TRANSFER)
                .status(Transaction.Status.SUCCESS)
                .amount(new BigDecimal("100.00"))
                .senderWallet(senderWallet)
                .receiverWallet(receiverWallet)
                .remark("Test transfer")
                .settlementDueDate(LocalDate.of(2026, 11, 6))
                .initiatedAt(LocalDateTime.of(2026, 11, 3, 10, 0))
                .settledAt(LocalDateTime.of(2026, 11, 3, 10, 5))
                .createdAt(LocalDateTime.of(2026, 11, 3, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 11, 3, 10, 5))
                .build();
    }

    private LedgerEntry createLedgerEntry(
            Long id,
            Transaction transaction,
            BigDecimal amount) {

        return LedgerEntry.builder()
                .id(id)
                .transaction(transaction)
                .wallet(senderWallet)
                .type(LedgerEntry.Type.DEBIT)
                .amount(amount)
                .balanceBefore(new BigDecimal("500.00"))
                .balanceAfter(new BigDecimal("400.00"))
                .createdAt(LocalDateTime.of(2026, 11, 3, 10, 0))
                .build();
    }

    @Test
    void listTransactions_NoFilters_ReturnsTransactions() throws Exception {
        when(transactionRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(transaction)));

        mockMvc.perform(get("/api/admin/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].type").value("P2P_TRANSFER"))
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.content[0].amount").value(100.00))
                .andExpect(jsonPath("$.content[0].senderEmail")
                        .value("sender@example.com"))
                .andExpect(jsonPath("$.content[0].receiverEmail")
                        .value("receiver@example.com"))
                .andExpect(jsonPath("$.content[0].settlementDueDate")
                        .value("2026-11-06"));

        verify(transactionRepository).findAll(any(Pageable.class));
    }

    @Test
    void listTransactions_EmptyResults() throws Exception {
        when(transactionRepository.findAll(any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/admin/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(transactionRepository).findAll(any(Pageable.class));
    }

    @Test
    void listTransactions_FilterByTypeAndStatus() throws Exception {
        when(transactionRepository.findByTypeAndStatus(
                eq("P2P_TRANSFER"),
                eq("SUCCESS"),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(transaction)));

        mockMvc.perform(get("/api/admin/transactions")
                        .param("type", "P2P_TRANSFER")
                        .param("status", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type")
                        .value("P2P_TRANSFER"))
                .andExpect(jsonPath("$.content[0].status")
                        .value("SUCCESS"));

        verify(transactionRepository).findByTypeAndStatus(
                eq("P2P_TRANSFER"),
                eq("SUCCESS"),
                any(Pageable.class));
    }

    @Test
    void listTransactions_FilterByAmountRange() throws Exception {
        when(transactionRepository.findByAmountRange(
                eq(new BigDecimal("50")),
                eq(new BigDecimal("200")),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(transaction)));

        mockMvc.perform(get("/api/admin/transactions")
                        .param("minAmount", "50")
                        .param("maxAmount", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].amount").value(100.00));

        verify(transactionRepository).findByAmountRange(
                eq(new BigDecimal("50")),
                eq(new BigDecimal("200")),
                any(Pageable.class));
    }

    @Test
    void listTransactions_FilterByDateRange() throws Exception {
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 30);

        when(transactionRepository.findByDateRange(
                eq(start),
                eq(end),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(transaction)));

        mockMvc.perform(get("/api/admin/transactions")
                        .param("startDate", "2026-11-01")
                        .param("endDate", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));

        verify(transactionRepository).findByDateRange(
                eq(start),
                eq(end),
                any(Pageable.class));
    }

    @Test
    void listTransactions_FilterByAllCriteria() throws Exception {
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 30);

        when(transactionRepository.findByAllFilters(
                eq("P2P_TRANSFER"),
                eq("SUCCESS"),
                eq(new BigDecimal("50")),
                eq(new BigDecimal("200")),
                eq(start),
                eq(end),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(transaction)));

        mockMvc.perform(get("/api/admin/transactions")
                        .param("type", "P2P_TRANSFER")
                        .param("status", "SUCCESS")
                        .param("minAmount", "50")
                        .param("maxAmount", "200")
                        .param("startDate", "2026-11-01")
                        .param("endDate", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"));

        verify(transactionRepository).findByAllFilters(
                eq("P2P_TRANSFER"),
                eq("SUCCESS"),
                eq(new BigDecimal("50")),
                eq(new BigDecimal("200")),
                eq(start),
                eq(end),
                any(Pageable.class));
    }

    @Test
    void listTransactions_InvalidDate_FallsBackToAllTransactions()
            throws Exception {

        when(transactionRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(transaction)));

        mockMvc.perform(get("/api/admin/transactions")
                        .param("startDate", "invalid-date")
                        .param("endDate", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));

        verify(transactionRepository).findAll(any(Pageable.class));
    }

    @Test
    void listTransactions_CreatedAtSort_UsesInitiatedAt()
            throws Exception {

        when(transactionRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(transaction)));

        mockMvc.perform(get("/api/admin/transactions")
                        .param("sortBy", "createdAt")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(transactionRepository).findAll(captor.capture());

        Pageable pageable = captor.getValue();

        Sort.Order order = pageable.getSort().getOrderFor("initiatedAt");

        assertNotNull(order);
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    void listTransactions_UnknownSortField_UsesDefaultSort()
            throws Exception {

        when(transactionRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(transaction)));

        mockMvc.perform(get("/api/admin/transactions")
                        .param("sortBy", "unknownField")
                        .param("sortDir", "invalid"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(transactionRepository).findAll(captor.capture());

        Pageable pageable = captor.getValue();

        Sort.Order order = pageable.getSort().getOrderFor("initiatedAt");

        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    void listTransactions_LimitsPageSizeTo100() throws Exception {
        when(transactionRepository.findAll(any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/admin/transactions")
                        .param("page", "0")
                        .param("size", "200"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(transactionRepository).findAll(captor.capture());

        assertEquals(100, captor.getValue().getPageSize());
    }

    @Test
    void getTransactionDetail_SuccessWithLedgerEntries()
            throws Exception {

        LedgerEntry entry = createLedgerEntry(
                10L,
                transaction,
                new BigDecimal("100.00"));

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(transaction));

        when(ledgerEntryRepository.findAll())
                .thenReturn(List.of(entry));

        mockMvc.perform(get("/api/admin/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("P2P_TRANSFER"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.senderEmail")
                        .value("sender@example.com"))
                .andExpect(jsonPath("$.receiverEmail")
                        .value("receiver@example.com"))
                .andExpect(jsonPath("$.settlementDueDate")
                        .value("2026-11-06"))
                .andExpect(jsonPath("$.ledgerEntries[0].id").value(10))
                .andExpect(jsonPath("$.ledgerEntries[0].type").value("DEBIT"))
                .andExpect(jsonPath("$.ledgerEntries[0].amount").value(100.00))
                .andExpect(jsonPath("$.ledgerEntries[0].balanceBefore")
                        .value(500.00))
                .andExpect(jsonPath("$.ledgerEntries[0].balanceAfter")
                        .value(400.00));

        verify(transactionRepository).findById(1L);
        verify(ledgerEntryRepository).findAll();
    }

    @Test
    void getTransactionDetail_NotFound() throws Exception {
        when(transactionRepository.findById(99L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/transactions/99"))
                .andExpect(status().isNotFound());

        verify(transactionRepository).findById(99L);
        verifyNoInteractions(ledgerEntryRepository);
    }

    @Test
    void getTransactionDetail_NullWalletsAndSettlementDate()
            throws Exception {

        Transaction systemTransaction = Transaction.builder()
                .id(2L)
                .requestId("system-request")
                .type(Transaction.Type.WALLET_TOP_UP)
                .status(Transaction.Status.PENDING)
                .amount(new BigDecimal("50.00"))
                .senderWallet(null)
                .receiverWallet(null)
                .remark("System transaction")
                .settlementDueDate(null)
                .initiatedAt(LocalDateTime.of(2026, 11, 3, 11, 0))
                .createdAt(LocalDateTime.of(2026, 11, 3, 11, 0))
                .updatedAt(LocalDateTime.of(2026, 11, 3, 11, 0))
                .build();

        when(transactionRepository.findById(2L))
                .thenReturn(Optional.of(systemTransaction));

        when(ledgerEntryRepository.findAll())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/admin/transactions/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderEmail").value("System"))
                .andExpect(jsonPath("$.receiverEmail").value("Unknown"))
                .andExpect(jsonPath("$.settlementDueDate").doesNotExist())
                .andExpect(jsonPath("$.ledgerEntries").isArray())
                .andExpect(jsonPath("$.ledgerEntries").isEmpty());

        verify(transactionRepository).findById(2L);
        verify(ledgerEntryRepository).findAll();
    }

    @Test
    void getTransactionDetail_IgnoresLedgerEntriesForOtherTransactions()
            throws Exception {

        Transaction otherTransaction = createTransaction(99L);

        LedgerEntry matchingEntry = createLedgerEntry(
                10L,
                transaction,
                new BigDecimal("100.00"));

        LedgerEntry unrelatedEntry = createLedgerEntry(
                11L,
                otherTransaction,
                new BigDecimal("25.00"));

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(transaction));

        when(ledgerEntryRepository.findAll())
                .thenReturn(List.of(matchingEntry, unrelatedEntry));

        mockMvc.perform(get("/api/admin/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ledgerEntries.length()").value(1))
                .andExpect(jsonPath("$.ledgerEntries[0].id").value(10));

        verify(transactionRepository).findById(1L);
        verify(ledgerEntryRepository).findAll();
    }
}