package com.p2ppayment.controller;

import com.p2ppayment.dto.TransactionResponse;
import com.p2ppayment.entity.Transaction;
import com.p2ppayment.repository.LedgerEntryRepository;
import com.p2ppayment.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminTransactionControllerTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private AdminTransactionController controller;

    private Transaction transaction;
    private Page<Transaction> page;

    @BeforeEach
    void setUp() {
        transaction = new Transaction();
        transaction.setId(1L);
        transaction.setType(Transaction.Type.WALLET_TOP_UP);
        transaction.setStatus(Transaction.Status.PENDING);
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setRemark("test transfer");
        transaction.setInitiatedAt(LocalDateTime.of(2026, 1, 10, 10, 0));
        transaction.setSettledAt(null);
        transaction.setSettlementDueDate(LocalDate.of(2026, 1, 15));

        page = new PageImpl<>(List.of(transaction));
    }

    private void stubAllQueries() {
        lenient().when(transactionRepository.findByAllFilters(
                anyString(), anyString(), any(BigDecimal.class), any(BigDecimal.class),
                any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(page);

        lenient().when(transactionRepository.findByTypeStatusAndDateRange(
                anyString(), anyString(), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(page);

        lenient().when(transactionRepository.findByTypeStatusAndAmountRange(
                anyString(), anyString(), any(BigDecimal.class), any(BigDecimal.class), any(Pageable.class)))
                .thenReturn(page);

        lenient().when(transactionRepository.findByStatusAmountAndDateRange(
                anyString(), any(BigDecimal.class), any(BigDecimal.class),
                any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(page);

        lenient().when(transactionRepository.findByTypeAmountAndDateRange(
                anyString(), any(BigDecimal.class), any(BigDecimal.class),
                any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(page);

        lenient().when(transactionRepository.findByTypeAndStatus(
                anyString(), anyString(), any(Pageable.class))).thenReturn(page);

        lenient().when(transactionRepository.findByTypeAndAmountRange(
                anyString(), any(BigDecimal.class), any(BigDecimal.class), any(Pageable.class)))
                .thenReturn(page);

        lenient().when(transactionRepository.findByTypeAndDateRange(
                anyString(), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(page);

        lenient().when(transactionRepository.findByStatusAndAmountRange(
                anyString(), any(BigDecimal.class), any(BigDecimal.class), any(Pageable.class)))
                .thenReturn(page);

        lenient().when(transactionRepository.findByStatusAndDateRange(
                anyString(), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(page);

        lenient().when(transactionRepository.findByAmountAndDateRange(
                any(BigDecimal.class), any(BigDecimal.class), any(LocalDate.class),
                any(LocalDate.class), any(Pageable.class))).thenReturn(page);

        lenient().when(transactionRepository.findAllByType(anyString(), any(Pageable.class)))
                .thenReturn(page);

        lenient().when(transactionRepository.findAllByStatus(anyString(), any(Pageable.class)))
                .thenReturn(page);

        lenient().when(transactionRepository.findByAmountRange(
                any(BigDecimal.class), any(BigDecimal.class), any(Pageable.class))).thenReturn(page);

        lenient().when(transactionRepository.findByDateRange(
                any(LocalDate.class), any(LocalDate.class), any(Pageable.class))).thenReturn(page);

        lenient().when(transactionRepository.findAll(any(Pageable.class))).thenReturn(page);
    }

    private ResponseEntity<Page<TransactionResponse>> list(
            String type, String status, BigDecimal minAmount, BigDecimal maxAmount,
            String startDate, String endDate, String sortBy, String sortDir, int pageNo, int size) {
        return controller.listTransactions(type, status, minAmount, maxAmount,
                startDate, endDate, sortBy, sortDir, pageNo, size);
    }

    @Test
    void listTransactions_NoFilters_UsesFindAllAndReturnsOk() {
        stubAllQueries();

        ResponseEntity<Page<TransactionResponse>> response =
                list(null, null, null, null, null, null, "initiatedAt", "desc", 0, 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        verify(transactionRepository).findAll(any(Pageable.class));
    }

    @Test
    void listTransactions_AllFourFilters() {
        stubAllQueries();

        list("WALLET_TOP_UP", "PENDING", new BigDecimal("10"), new BigDecimal("200"),
                "2026-01-01", "2026-01-31", "createdAt", "asc", 0, 20);

        verify(transactionRepository).findByAllFilters(
                eq("WALLET_TOP_UP"), eq("PENDING"), eq(new BigDecimal("10")),
                eq(new BigDecimal("200")), eq(LocalDate.parse("2026-01-01")),
                eq(LocalDate.parse("2026-01-31")), any(Pageable.class));
    }

    @Test
    void listTransactions_TypeStatusDateFilters() {
        stubAllQueries();
        list("WALLET_TOP_UP", "PENDING", null, null,
                "2026-01-01", "2026-01-31", "amount", "desc", 0, 20);
        verify(transactionRepository).findByTypeStatusAndDateRange(
                eq("WALLET_TOP_UP"), eq("PENDING"), any(LocalDate.class),
                any(LocalDate.class), any(Pageable.class));
    }

    @Test
    void listTransactions_TypeStatusAmountFilters() {
        stubAllQueries();
        list("WALLET_TOP_UP", "PENDING", new BigDecimal("10"), new BigDecimal("200"),
                null, null, "type", "desc", 0, 20);
        verify(transactionRepository).findByTypeStatusAndAmountRange(
                eq("WALLET_TOP_UP"), eq("PENDING"), any(BigDecimal.class),
                any(BigDecimal.class), any(Pageable.class));
    }

    @Test
    void listTransactions_StatusAmountDateFilters() {
        stubAllQueries();
        list(null, "PENDING", new BigDecimal("10"), new BigDecimal("200"),
                "2026-01-01", "2026-01-31", "status", "desc", 0, 20);
        verify(transactionRepository).findByStatusAmountAndDateRange(
                eq("PENDING"), any(BigDecimal.class), any(BigDecimal.class),
                any(LocalDate.class), any(LocalDate.class), any(Pageable.class));
    }

    @Test
    void listTransactions_TypeAmountDateFilters() {
        stubAllQueries();
        list("WALLET_TOP_UP", null, new BigDecimal("10"), new BigDecimal("200"),
                "2026-01-01", "2026-01-31", "id", "desc", 0, 20);
        verify(transactionRepository).findByTypeAmountAndDateRange(
                eq("WALLET_TOP_UP"), any(BigDecimal.class), any(BigDecimal.class),
                any(LocalDate.class), any(LocalDate.class), any(Pageable.class));
    }

    @Test
    void listTransactions_TypeAndStatusFilters() {
        stubAllQueries();
        list("WALLET_TOP_UP", "PENDING", null, null, null, null,
                "settlementDueDate", "desc", 0, 20);
        verify(transactionRepository).findByTypeAndStatus(
                eq("WALLET_TOP_UP"), eq("PENDING"), any(Pageable.class));
    }

    @Test
    void listTransactions_TypeAndAmountFilters() {
        stubAllQueries();
        list("WALLET_TOP_UP", null, BigDecimal.ONE, BigDecimal.TEN,
                null, null, "amount", "desc", 0, 20);
        verify(transactionRepository).findByTypeAndAmountRange(
                eq("WALLET_TOP_UP"), any(BigDecimal.class), any(BigDecimal.class), any(Pageable.class));
    }

    @Test
    void listTransactions_TypeAndDateFilters() {
        stubAllQueries();
        list("WALLET_TOP_UP", null, null, null, "2026-01-01", "2026-01-31",
                "type", "desc", 0, 20);
        verify(transactionRepository).findByTypeAndDateRange(
                eq("WALLET_TOP_UP"), any(LocalDate.class), any(LocalDate.class), any(Pageable.class));
    }

    @Test
    void listTransactions_StatusAndAmountFilters() {
        stubAllQueries();
        list(null, "PENDING", BigDecimal.ONE, BigDecimal.TEN, null, null,
                "status", "desc", 0, 20);
        verify(transactionRepository).findByStatusAndAmountRange(
                eq("PENDING"), any(BigDecimal.class), any(BigDecimal.class), any(Pageable.class));
    }

    @Test
    void listTransactions_StatusAndDateFilters() {
        stubAllQueries();
        list(null, "PENDING", null, null, "2026-01-01", "2026-01-31",
                "id", "desc", 0, 20);
        verify(transactionRepository).findByStatusAndDateRange(
                eq("PENDING"), any(LocalDate.class), any(LocalDate.class), any(Pageable.class));
    }

    @Test
    void listTransactions_AmountAndDateFilters() {
        stubAllQueries();
        list(null, null, BigDecimal.ONE, BigDecimal.TEN, "2026-01-01", "2026-01-31",
                "createdAt", "desc", 0, 20);
        verify(transactionRepository).findByAmountAndDateRange(
                any(BigDecimal.class), any(BigDecimal.class), any(LocalDate.class),
                any(LocalDate.class), any(Pageable.class));
    }

    @Test
    void listTransactions_TypeOnly() {
        stubAllQueries();
        list("WALLET_TOP_UP", null, null, null, null, null, "initiatedAt", "desc", 0, 20);
        verify(transactionRepository).findAllByType(eq("WALLET_TOP_UP"), any(Pageable.class));
    }

    @Test
    void listTransactions_StatusOnly() {
        stubAllQueries();
        list(null, "PENDING", null, null, null, null, "initiatedAt", "desc", 0, 20);
        verify(transactionRepository).findAllByStatus(eq("PENDING"), any(Pageable.class));
    }

    @Test
    void listTransactions_AmountOnly() {
        stubAllQueries();
        list(null, null, BigDecimal.ONE, BigDecimal.TEN, null, null, "initiatedAt", "desc", 0, 20);
        verify(transactionRepository).findByAmountRange(
                any(BigDecimal.class), any(BigDecimal.class), any(Pageable.class));
    }

    @Test
    void listTransactions_DateOnly() {
        stubAllQueries();
        list(null, null, null, null, "2026-01-01", "2026-01-31", "initiatedAt", "desc", 0, 20);
        verify(transactionRepository).findByDateRange(
                any(LocalDate.class), any(LocalDate.class), any(Pageable.class));
    }

    @Test
    void listTransactions_InvalidDate_FallsBackToFindAll() {
        stubAllQueries();

        list(null, null, null, null, "not-a-date", "2026-01-31",
                "initiatedAt", "desc", 0, 20);

        verify(transactionRepository).findAll(any(Pageable.class));
    }

    @Test
    void listTransactions_OnlyOneAmountBound_DoesNotApplyAmountFilter() {
        stubAllQueries();

        list(null, null, BigDecimal.ONE, null, null, null,
                "initiatedAt", "desc", 0, 20);

        verify(transactionRepository).findAll(any(Pageable.class));
    }

    @Test
    void listTransactions_OnlyOneDateBound_DoesNotApplyDateFilter() {
        stubAllQueries();

        list(null, null, null, null, "2026-01-01", null,
                "initiatedAt", "desc", 0, 20);

        verify(transactionRepository).findAll(any(Pageable.class));
    }

    @Test
    void listTransactions_CapsPageSizeAt100() {
        stubAllQueries();

        list(null, null, null, null, null, null,
                "initiatedAt", "asc", 0, 500);

        org.mockito.ArgumentCaptor<Pageable> captor =
                org.mockito.ArgumentCaptor.forClass(Pageable.class);

        verify(transactionRepository).findAll(captor.capture());

        Pageable pageable = captor.getValue();

        assertEquals(100, pageable.getPageSize());
        assertEquals(0, pageable.getPageNumber());
        assertFalse(pageable.getSort().isUnsorted());
        assertEquals(
                org.springframework.data.domain.Sort.Direction.ASC,
                pageable.getSort().iterator().next().getDirection()
        );
    }

    @Test
    void getTransactionDetail_NotFound_ThrowsNotFound() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.getTransactionDetail(99L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Transaction not found", exception.getReason());
    }

    @Test
    void getTransactionDetail_FoundWithNoWalletsAndNoLedgerEntries() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        when(ledgerEntryRepository.findAll()).thenReturn(List.of());

        ResponseEntity<?> response = controller.getTransactionDetail(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void getTransactionDetail_FiltersLedgerEntriesForTransaction() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        com.p2ppayment.entity.LedgerEntry entry = mock(com.p2ppayment.entity.LedgerEntry.class);
        when(entry.getTransaction()).thenReturn(transaction);
        when(entry.getId()).thenReturn(11L);
        when(entry.getType()).thenReturn(com.p2ppayment.entity.LedgerEntry.Type.CREDIT);
        when(entry.getAmount()).thenReturn(new BigDecimal("100.00"));
        when(entry.getBalanceBefore()).thenReturn(BigDecimal.ZERO);
        when(entry.getBalanceAfter()).thenReturn(new BigDecimal("100.00"));
        when(entry.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 1, 10, 10, 1));
        when(ledgerEntryRepository.findAll()).thenReturn(List.of(entry));

        ResponseEntity<?> response = controller.getTransactionDetail(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(ledgerEntryRepository).findAll();
    }
}
