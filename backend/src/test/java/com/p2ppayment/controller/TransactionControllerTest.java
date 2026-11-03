
package com.p2ppayment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2ppayment.dto.SendMoneyRequest;
import com.p2ppayment.entity.LedgerEntry;
import com.p2ppayment.entity.Transaction;
import com.p2ppayment.entity.User;
import com.p2ppayment.entity.Wallet;
import com.p2ppayment.repository.LedgerEntryRepository;
import com.p2ppayment.service.TransactionService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private LedgerEntryRepository ledgerEntryRepository;

    private static final Long USER_ID = 1L;

    private static final LocalDateTime INITIATED_AT =
            LocalDateTime.of(2026, 11, 3, 10, 0);

    private static final LocalDateTime SETTLED_AT =
            LocalDateTime.of(2026, 11, 3, 11, 0);

    @BeforeEach
    void setUp() {
        setAuthentication("1");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(String username) {
        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        Collections.emptyList()
                )
        );

        SecurityContextHolder.setContext(context);
    }

    private Transaction createTransaction() {
        return Transaction.builder()
                .id(10L)
                .requestId("request-10")
                .type(Transaction.Type.P2P_TRANSFER)
                .status(Transaction.Status.SUCCESS)
                .amount(new BigDecimal("250.00"))
                .remark("Test payment")
                .settlementDueDate(LocalDate.of(2026, 11, 6))
                .initiatedAt(INITIATED_AT)
                .settledAt(SETTLED_AT)
                .createdAt(INITIATED_AT)
                .updatedAt(SETTLED_AT)
                .build();
    }

    private User createUser(Long id, String name, String email) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        return user;
    }

    private Wallet createWallet(Long id, User user, String balance) {
        Wallet wallet = new Wallet();
        wallet.setId(id);
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal(balance));
        return wallet;
    }

    private LedgerEntry createLedgerEntry(
            Long id, Transaction transaction) {

        return LedgerEntry.builder()
                .id(id)
                .transaction(transaction)
                .type(LedgerEntry.Type.DEBIT)
                .amount(new BigDecimal("250.00"))
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("750.00"))
                .createdAt(INITIATED_AT)
                .build();
    }

    // ---------------------------------------------------------
    // Transaction history
    // ---------------------------------------------------------

    @Test
    void getTransactionHistory_EmptyPage() throws Exception {
        Page<Transaction> page =
                new PageImpl<>(Collections.emptyList());

        when(transactionService.getTransactionHistory(USER_ID, 0, 20))
                .thenReturn(page);

        mockMvc.perform(get("/api/transactions")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(transactionService)
                .getTransactionHistory(USER_ID, 0, 20);
    }

    @Test
    void getTransactionHistory_DefaultPagination() throws Exception {
        when(transactionService.getTransactionHistory(USER_ID, 0, 20))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(transactionService)
                .getTransactionHistory(USER_ID, 0, 20);
    }

    @Test
    void getTransactionHistory_PopulatedPageWithNullWallets()
            throws Exception {

        Transaction transaction = createTransaction();

        when(transactionService.getTransactionHistory(USER_ID, 0, 20))
                .thenReturn(new PageImpl<>(List.of(transaction)));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].type")
                        .value("P2P_TRANSFER"))
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.content[0].amount").value(250.00))
                .andExpect(jsonPath("$.content[0].senderEmail")
                        .value("System"))
                .andExpect(jsonPath("$.content[0].receiverEmail")
                        .value("System"))
                .andExpect(jsonPath("$.content[0].remark")
                        .value("Test payment"))
                .andExpect(jsonPath("$.content[0].settlementDueDate")
                        .value("2026-11-06"));

        verify(transactionService)
                .getTransactionHistory(USER_ID, 0, 20);
    }

    @Test
    void getTransactionHistory_WithWalletUsers() throws Exception {
        User sender = createUser(1L, "Sender", "sender@example.com");
        User receiver = createUser(2L, "Receiver", "receiver@example.com");

        Transaction transaction = createTransaction();
        transaction.setSenderWallet(createWallet(1L, sender, "750.00"));
        transaction.setReceiverWallet(createWallet(2L, receiver, "250.00"));

        when(transactionService.getTransactionHistory(USER_ID, 0, 20))
                .thenReturn(new PageImpl<>(List.of(transaction)));

        mockMvc.perform(get("/api/transactions")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].senderEmail")
                        .value("sender@example.com"))
                .andExpect(jsonPath("$.content[0].receiverEmail")
                        .value("receiver@example.com"));

        verify(transactionService)
                .getTransactionHistory(USER_ID, 0, 20);
    }

    // ---------------------------------------------------------
    // Send money
    // ---------------------------------------------------------

    @Test
    void sendMoney_Success() throws Exception {
        SendMoneyRequest request = new SendMoneyRequest(
                "receiver@example.com",
                new BigDecimal("250.00"),
                "Test payment",
                "1234"
        );

        User receiver = createUser(
                2L, "Receiver User", "receiver@example.com");

        Wallet senderWallet =
                createWallet(1L, null, "750.00");

        Wallet receiverWallet =
                createWallet(2L, receiver, "250.00");

        Transaction transaction = createTransaction();
        transaction.setSenderWallet(senderWallet);
        transaction.setReceiverWallet(receiverWallet);

        when(transactionService.sendMoney(
                eq(USER_ID),
                eq("receiver@example.com"),
                eq(new BigDecimal("250.00")),
                eq("Test payment"),
                eq("1234")
        )).thenReturn(transaction);

        mockMvc.perform(post("/api/transactions/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(10))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(250.00))
                .andExpect(jsonPath("$.senderNewBalance").value(750.00))
                .andExpect(jsonPath("$.settlementDueDate")
                        .value("2026-11-06"))
                .andExpect(jsonPath("$.recipientName")
                        .value("Receiver User"))
                .andExpect(jsonPath("$.recipientEmail")
                        .value("receiver@example.com"))
                .andExpect(jsonPath("$.message")
                        .value("Money sent successfully. Settlement expected by 2026-11-06"));

        verify(transactionService).sendMoney(
                USER_ID,
                "receiver@example.com",
                new BigDecimal("250.00"),
                "Test payment",
                "1234"
        );
    }

    @Test
    void sendMoney_InvalidAmount_ReturnsBadRequest()
            throws Exception {

        SendMoneyRequest request = new SendMoneyRequest(
                "receiver@example.com",
                BigDecimal.ZERO,
                "Test payment",
                "1234"
        );

        mockMvc.perform(post("/api/transactions/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    @Test
    void sendMoney_MissingRecipient_ReturnsBadRequest()
            throws Exception {

        SendMoneyRequest request = new SendMoneyRequest(
                " ",
                new BigDecimal("250.00"),
                "Test payment",
                "1234"
        );

        mockMvc.perform(post("/api/transactions/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    @Test
    void sendMoney_MissingRemark_ReturnsBadRequest()
            throws Exception {

        SendMoneyRequest request = new SendMoneyRequest(
                "receiver@example.com",
                new BigDecimal("250.00"),
                " ",
                "1234"
        );

        mockMvc.perform(post("/api/transactions/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    @Test
    void sendMoney_MissingPin_ReturnsBadRequest()
            throws Exception {

        SendMoneyRequest request = new SendMoneyRequest(
                "receiver@example.com",
                new BigDecimal("250.00"),
                "Test payment",
                " "
        );

        mockMvc.perform(post("/api/transactions/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    @Test
    void sendMoney_NullReceiverWallet_UsesFallbackRecipientDetails()
            throws Exception {

        SendMoneyRequest request = new SendMoneyRequest(
                "receiver@example.com",
                new BigDecimal("250.00"),
                "Test payment",
                "1234"
        );

        Transaction transaction = createTransaction();
        transaction.setSenderWallet(createWallet(1L, null, "750.00"));
        transaction.setReceiverWallet(null);

        when(transactionService.sendMoney(
                USER_ID,
                "receiver@example.com",
                new BigDecimal("250.00"),
                "Test payment",
                "1234"
        )).thenReturn(transaction);

        mockMvc.perform(post("/api/transactions/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientName").value("Unknown"))
                .andExpect(jsonPath("$.recipientEmail")
                        .value("receiver@example.com"));

        verify(transactionService).sendMoney(
                USER_ID,
                "receiver@example.com",
                new BigDecimal("250.00"),
                "Test payment",
                "1234"
        );
    }

    @Test
    void sendMoney_ReceiverHasNoUser_UsesFallbackRecipientDetails()
            throws Exception {

        SendMoneyRequest request = new SendMoneyRequest(
                "receiver@example.com",
                new BigDecimal("250.00"),
                "Test payment",
                "1234"
        );

        Transaction transaction = createTransaction();
        transaction.setSenderWallet(createWallet(1L, null, "750.00"));
        transaction.setReceiverWallet(createWallet(2L, null, "250.00"));

        when(transactionService.sendMoney(
                USER_ID,
                "receiver@example.com",
                new BigDecimal("250.00"),
                "Test payment",
                "1234"
        )).thenReturn(transaction);

        mockMvc.perform(post("/api/transactions/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientName").value("Unknown"))
                .andExpect(jsonPath("$.recipientEmail")
                        .value("receiver@example.com"));
    }

    // ---------------------------------------------------------
    // Mini-statement
    // ---------------------------------------------------------

    @Test
    void getMiniStatement_EmptyList() throws Exception {
        when(transactionService.getMiniStatement(USER_ID))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/transactions/mini-statement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(transactionService).getMiniStatement(USER_ID);
    }

    @Test
    void getMiniStatement_WithTransactions() throws Exception {
        Transaction transaction = createTransaction();

        when(transactionService.getMiniStatement(USER_ID))
                .thenReturn(List.of(transaction));

        mockMvc.perform(get("/api/transactions/mini-statement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].senderEmail").value("System"))
                .andExpect(jsonPath("$[0].receiverEmail").value("System"));

        verify(transactionService).getMiniStatement(USER_ID);
    }

    // ---------------------------------------------------------
    // Transaction details and ledger entries
    // ---------------------------------------------------------

    @Test
    void getTransactionDetail_WithLedgerEntry() throws Exception {
        Transaction transaction = createTransaction();

        LedgerEntry entry = createLedgerEntry(101L, transaction);

        when(transactionService.getTransactionDetail(10L, USER_ID))
                .thenReturn(transaction);

        when(ledgerEntryRepository.findAll())
                .thenReturn(List.of(entry));

        mockMvc.perform(get("/api/transactions/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.type").value("P2P_TRANSFER"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(250.00))
                .andExpect(jsonPath("$.senderEmail").value("System"))
                .andExpect(jsonPath("$.receiverEmail").value("System"))
                .andExpect(jsonPath("$.settlementDueDate")
                        .value("2026-11-06"))
                .andExpect(jsonPath("$.ledgerEntries.length()").value(1))
                .andExpect(jsonPath("$.ledgerEntries[0].id").value(101))
                .andExpect(jsonPath("$.ledgerEntries[0].type").value("DEBIT"))
                .andExpect(jsonPath("$.ledgerEntries[0].amount").value(250.00))
                .andExpect(jsonPath("$.ledgerEntries[0].balanceBefore")
                        .value(1000.00))
                .andExpect(jsonPath("$.ledgerEntries[0].balanceAfter")
                        .value(750.00));

        verify(transactionService).getTransactionDetail(10L, USER_ID);
        verify(ledgerEntryRepository).findAll();
    }

    @Test
    void getTransactionDetail_FiltersLedgerEntriesByTransactionId()
            throws Exception {

        Transaction requestedTransaction = createTransaction();

        Transaction otherTransaction = Transaction.builder()
                .id(20L)
                .type(Transaction.Type.P2P_TRANSFER)
                .status(Transaction.Status.SUCCESS)
                .amount(new BigDecimal("50.00"))
                .initiatedAt(INITIATED_AT)
                .build();

        LedgerEntry matchingEntry =
                createLedgerEntry(101L, requestedTransaction);

        LedgerEntry otherEntry =
                createLedgerEntry(202L, otherTransaction);

        when(transactionService.getTransactionDetail(10L, USER_ID))
                .thenReturn(requestedTransaction);

        when(ledgerEntryRepository.findAll())
                .thenReturn(List.of(matchingEntry, otherEntry));

        mockMvc.perform(get("/api/transactions/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ledgerEntries.length()").value(1))
                .andExpect(jsonPath("$.ledgerEntries[0].id").value(101));

        verify(transactionService).getTransactionDetail(10L, USER_ID);
    }

    @Test
    void getTransactionDetail_EmptyLedgerEntriesAndNullSettlementDate()
            throws Exception {

        Transaction transaction = createTransaction();
        transaction.setSettlementDueDate(null);
        transaction.setSettledAt(null);

        when(transactionService.getTransactionDetail(10L, USER_ID))
                .thenReturn(transaction);

        when(ledgerEntryRepository.findAll())
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/transactions/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settlementDueDate").doesNotExist())
                .andExpect(jsonPath("$.settledAt").doesNotExist())
                .andExpect(jsonPath("$.ledgerEntries").isEmpty());

        verify(transactionService).getTransactionDetail(10L, USER_ID);
    }

    @Test
    void getTransactionDetail_WalletsWithNullUsers_UsesFallbackEmails()
            throws Exception {

        Transaction transaction = createTransaction();
        transaction.setSenderWallet(createWallet(1L, null, "750.00"));
        transaction.setReceiverWallet(createWallet(2L, null, "250.00"));

        when(transactionService.getTransactionDetail(10L, USER_ID))
                .thenReturn(transaction);

        when(ledgerEntryRepository.findAll())
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/transactions/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderEmail").value("System"))
                .andExpect(jsonPath("$.receiverEmail").value("System"));

        verify(transactionService).getTransactionDetail(10L, USER_ID);
    }

    // ---------------------------------------------------------
    // Transaction statistics
    // ---------------------------------------------------------

    @Test
    void getTransactionStats_ReturnsAllStatistics() throws Exception {
        TransactionService.TransactionStats stats =
                new TransactionService.TransactionStats(
                        12L,
                        8L,
                        new BigDecimal("3500.00"),
                        new BigDecimal("2200.00"),
                        4L,
                        3L,
                        new BigDecimal("1000.00"),
                        new BigDecimal("700.00")
                );

        when(transactionService.getTransactionStats(USER_ID))
                .thenReturn(stats);

        mockMvc.perform(get("/api/transactions/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSent").value(12))
                .andExpect(jsonPath("$.totalReceived").value(8))
                .andExpect(jsonPath("$.totalSentAmount").value(3500.00))
                .andExpect(jsonPath("$.totalReceivedAmount").value(2200.00))
                .andExpect(jsonPath("$.thisMonthSentCount").value(4))
                .andExpect(jsonPath("$.thisMonthReceivedCount").value(3))
                .andExpect(jsonPath("$.thisMonthSentAmount").value(1000.00))
                .andExpect(jsonPath("$.thisMonthReceivedAmount").value(700.00));

        verify(transactionService).getTransactionStats(USER_ID);
    }

    @Test
    void getTransactionStats_ZeroStatistics() throws Exception {
        TransactionService.TransactionStats stats =
                new TransactionService.TransactionStats(
                        0L,
                        0L,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        0L,
                        0L,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                );

        when(transactionService.getTransactionStats(USER_ID))
                .thenReturn(stats);

        mockMvc.perform(get("/api/transactions/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSent").value(0))
                .andExpect(jsonPath("$.totalReceived").value(0))
                .andExpect(jsonPath("$.totalSentAmount").value(0))
                .andExpect(jsonPath("$.totalReceivedAmount").value(0))
                .andExpect(jsonPath("$.thisMonthSentCount").value(0))
                .andExpect(jsonPath("$.thisMonthReceivedCount").value(0))
                .andExpect(jsonPath("$.thisMonthSentAmount").value(0))
                .andExpect(jsonPath("$.thisMonthReceivedAmount").value(0));
    }

    // ---------------------------------------------------------
    // Authentication ID validation
    // ---------------------------------------------------------

    @Test
    void getTransactionHistory_InvalidUserId_ReturnsUnauthorized()
            throws Exception {

        setAuthentication("not-a-number");

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(transactionService);
    }

    @Test
    void getTransactionHistory_MissingAuthentication_ReturnsUnauthorized()
            throws Exception {

        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(transactionService);
    }

    @Test
    void getTransactionHistory_NullAuthentication_ReturnsUnauthorized()
            throws Exception {

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(null);
        SecurityContextHolder.setContext(context);

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(transactionService);
    }
}
