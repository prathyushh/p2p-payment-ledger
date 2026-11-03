package com.p2ppayment.controller;

import com.p2ppayment.dto.TransactionDetailResponse;
import com.p2ppayment.dto.TransactionResponse;
import com.p2ppayment.service.AdminTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminTransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminTransactionService adminTransactionService;

    private TransactionResponse transactionResponse;

    @BeforeEach
    void setUp() {
        transactionResponse = new TransactionResponse(
            1L,
            1L,
            2L,
            "sender@example.com",
            "receiver@example.com",
            new BigDecimal("1000.00"),
            "PENDING",
            "P2P_TRANSFER",
            "Payment for services",
            LocalDateTime.now(),
            null,
            null
        );
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void listAllTransactions_Success() throws Exception {
        // Arrange
        Page<TransactionResponse> page = new PageImpl<>(Arrays.asList(transactionResponse));
        when(adminTransactionService.listAllTransactions(any(), anyString(), anyString(), any(), any()))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/transactions")
                .param("page", "0")
                .param("size", "20")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].amount").value(1000.00))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void listAllTransactions_WithFilters_Success() throws Exception {
        // Arrange
        Page<TransactionResponse> page = new PageImpl<>(Arrays.asList(transactionResponse));
        when(adminTransactionService.listAllTransactions(any(), anyString(), anyString(), any(), any()))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/transactions")
                .param("page", "0")
                .param("size", "20")
                .param("status", "PENDING")
                .param("type", "P2P_TRANSFER")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void listAllTransactions_WithSorting_Success() throws Exception {
        // Arrange
        Page<TransactionResponse> page = new PageImpl<>(Arrays.asList(transactionResponse));
        when(adminTransactionService.listAllTransactions(any(), anyString(), anyString(), any(), any()))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/transactions")
                .param("page", "0")
                .param("size", "20")
                .param("sortBy", "initiatedAt")
                .param("sortDir", "desc")
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void getTransactionDetails_Success() throws Exception {
        // Arrange
        TransactionDetailResponse detailResponse = new TransactionDetailResponse(
            transactionResponse,
            Arrays.asList(),
            Arrays.asList()
        );
        when(adminTransactionService.getTransactionDetails(anyLong())).thenReturn(detailResponse);

        // Act & Assert
        mockMvc.perform(get("/api/admin/transactions/1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.id").value(1))
                .andExpect(jsonPath("$.transaction.amount").value(1000.00));
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void listAllTransactions_NonAdmin_ReturnsForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/transactions")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
