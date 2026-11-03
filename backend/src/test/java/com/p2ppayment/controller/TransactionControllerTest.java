package com.p2ppayment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2ppayment.dto.*;
import com.p2ppayment.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    private SendMoneyRequest sendMoneyRequest;
    private SendMoneyResponse sendMoneyResponse;
    private TransactionResponse transactionResponse;
    private RecipientSearchResponse recipientSearchResponse;

    @BeforeEach
    void setUp() {
        sendMoneyRequest = new SendMoneyRequest(
            2L,
            new BigDecimal("1000.00"),
            "1234",
            "Payment for services",
            "request-id-123"
        );

        sendMoneyResponse = new SendMoneyResponse(
            1L,
            "PENDING",
            new BigDecimal("1000.00"),
            new BigDecimal("4000.00"),
            "Transaction initiated successfully"
        );

        transactionResponse = new TransactionResponse(
            1L,
            1L,
            2L,
            "sender@example.com",
            "recipient@example.com",
            new BigDecimal("1000.00"),
            "PENDING",
            "P2P_TRANSFER",
            "Payment for services",
            LocalDateTime.now(),
            null,
            null
        );

        recipientSearchResponse = new RecipientSearchResponse(
            2L,
            "recipient@example.com",
            "Recipient User"
        );
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void sendMoney_Success() throws Exception {
        // Arrange
        when(transactionService.sendMoney(anyLong(), any(SendMoneyRequest.class)))
            .thenReturn(sendMoneyResponse);

        // Act & Assert
        mockMvc.perform(post("/api/transactions/send")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sendMoneyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.amount").value(1000.00));
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void sendMoney_InvalidAmount_ReturnsBadRequest() throws Exception {
        // Arrange
        SendMoneyRequest invalidRequest = new SendMoneyRequest(
            2L,
            BigDecimal.ZERO,
            "1234",
            "Payment",
            "request-id-123"
        );

        // Act & Assert
        mockMvc.perform(post("/api/transactions/send")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void getTransactionHistory_Success() throws Exception {
        // Arrange
        Page<TransactionResponse> page = new PageImpl<>(Arrays.asList(transactionResponse));
        when(transactionService.getTransactionHistory(anyLong(), any(), any(), any(), any(), any()))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/transactions/history")
                .param("page", "0")
                .param("size", "20")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].amount").value(1000.00));
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void getTransactionById_Success() throws Exception {
        // Arrange
        TransactionDetailResponse detailResponse = new TransactionDetailResponse(
            transactionResponse,
            Arrays.asList(),
            Arrays.asList()
        );
        when(transactionService.getTransactionById(anyLong(), anyLong()))
            .thenReturn(detailResponse);

        // Act & Assert
        mockMvc.perform(get("/api/transactions/1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.id").value(1));
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void searchRecipients_Success() throws Exception {
        // Arrange
        List<RecipientSearchResponse> recipients = Arrays.asList(recipientSearchResponse);
        when(transactionService.searchRecipients(anyString()))
            .thenReturn(recipients);

        // Act & Assert
        mockMvc.perform(get("/api/transactions/recipients/search")
                .param("query", "recipient")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(2))
                .andExpect(jsonPath("$[0].email").value("recipient@example.com"));
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void searchRecipients_EmptyQuery_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/transactions/recipients/search")
                .param("query", "")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}
