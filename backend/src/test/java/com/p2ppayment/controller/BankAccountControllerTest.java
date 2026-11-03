package com.p2ppayment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2ppayment.dto.*;
import com.p2ppayment.service.BankAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

@WebMvcTest(BankAccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class BankAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BankAccountService bankAccountService;

    private BankAccountRequest bankAccountRequest;
    private BankAccountResponse bankAccountResponse;
    private AddDemoFundsRequest addDemoFundsRequest;

    @BeforeEach
    void setUp() {
        bankAccountRequest = new BankAccountRequest(
            "Test Bank",
            "My Savings Account"
        );

        bankAccountResponse = new BankAccountResponse(
            1L,
            "Test Bank",
            "My Savings Account",
            "DEMO-1234567890",
            new BigDecimal("5000.00"),
            true,
            LocalDateTime.now()
        );

        addDemoFundsRequest = new AddDemoFundsRequest(new BigDecimal("2000.00"));
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void createBankAccount_Success() throws Exception {
        // Arrange
        when(bankAccountService.createBankAccount(anyLong(), anyString(), anyString()))
            .thenReturn(bankAccountResponse);

        // Act & Assert
        mockMvc.perform(post("/api/bank-accounts")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bankAccountRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bankName").value("Test Bank"))
                .andExpect(jsonPath("$.accountNickname").value("My Savings Account"));
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void createBankAccount_MissingFields_ReturnsBadRequest() throws Exception {
        // Arrange
        BankAccountRequest invalidRequest = new BankAccountRequest("", "");

        // Act & Assert
        mockMvc.perform(post("/api/bank-accounts")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void listBankAccounts_Success() throws Exception {
        // Arrange
        List<BankAccountResponse> accounts = Arrays.asList(bankAccountResponse);
        when(bankAccountService.listBankAccounts(anyLong())).thenReturn(accounts);

        // Act & Assert
        mockMvc.perform(get("/api/bank-accounts")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].bankName").value("Test Bank"));
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void getBankAccount_Success() throws Exception {
        // Arrange
        when(bankAccountService.getBankAccount(anyLong(), anyLong()))
            .thenReturn(bankAccountResponse);

        // Act & Assert
        mockMvc.perform(get("/api/bank-accounts/1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accountIdentifier").value("DEMO-1234567890"));
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void deactivateBankAccount_Success() throws Exception {
        // Arrange
        BankAccountResponse deactivatedAccount = new BankAccountResponse(
            1L,
            "Test Bank",
            "My Savings Account",
            "DEMO-1234567890",
            new BigDecimal("5000.00"),
            false,
            LocalDateTime.now()
        );
        when(bankAccountService.deactivateBankAccount(anyLong(), anyLong()))
            .thenReturn(deactivatedAccount);

        // Act & Assert
        mockMvc.perform(post("/api/bank-accounts/1/deactivate")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void addDemoFunds_Success() throws Exception {
        // Arrange
        BankAccountResponse updatedAccount = new BankAccountResponse(
            1L,
            "Test Bank",
            "My Savings Account",
            "DEMO-1234567890",
            new BigDecimal("7000.00"),
            true,
            LocalDateTime.now()
        );
        when(bankAccountService.addDemoFunds(anyLong(), anyLong(), any(BigDecimal.class)))
            .thenReturn(updatedAccount);

        // Act & Assert
        mockMvc.perform(post("/api/bank-accounts/1/add-demo-funds")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addDemoFundsRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulatedBalance").value(7000.00));
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void addDemoFunds_InvalidAmount_ReturnsBadRequest() throws Exception {
        // Arrange
        AddDemoFundsRequest invalidRequest = new AddDemoFundsRequest(BigDecimal.ZERO);

        // Act & Assert
        mockMvc.perform(post("/api/bank-accounts/1/add-demo-funds")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
