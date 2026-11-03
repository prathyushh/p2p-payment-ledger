
package com.p2ppayment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2ppayment.dto.AddDemoFundsRequest;
import com.p2ppayment.dto.BankAccountRequest;
import com.p2ppayment.entity.SimulatedBankAccount;
import com.p2ppayment.service.BankAccountService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BankAccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class BankAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BankAccountService bankAccountService;

    private SimulatedBankAccount createAccount(
            Long id,
            BigDecimal balance,
            Boolean active) {

        LocalDateTime now = LocalDateTime.now();

        return SimulatedBankAccount.builder()
                .id(id)
                .bankName("Test Bank")
                .accountNickname("My Savings Account")
                .accountIdentifier("DEMO-1234567890")
                .simulatedBalance(balance)
                .isActive(active)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private void setAuthenticatedUser() {
        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "1", null, Collections.emptyList()));

        SecurityContextHolder.setContext(context);
    }

    @BeforeEach
    void setUp() {
        setAuthenticatedUser();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createBankAccount_Success() throws Exception {
        BankAccountRequest request = new BankAccountRequest(
                "Test Bank",
                "My Savings Account");

        SimulatedBankAccount account = createAccount(
                1L, new BigDecimal("5000.00"), true);

        when(bankAccountService.createBankAccount(
                anyLong(), anyString(), anyString()))
                .thenReturn(account);

        mockMvc.perform(post("/api/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bankName").value("Test Bank"))
                .andExpect(jsonPath("$.accountNickname")
                        .value("My Savings Account"))
                .andExpect(jsonPath("$.accountIdentifier")
                        .value("DEMO-1234567890"))
                .andExpect(jsonPath("$.simulatedBalance")
                        .value(5000.00))
                .andExpect(jsonPath("$.isActive").value(true));

        verify(bankAccountService)
                .createBankAccount(
                        1L, "Test Bank", "My Savings Account");
    }

    @Test
    void listBankAccounts_Success() throws Exception {
        SimulatedBankAccount account = createAccount(
                1L, new BigDecimal("5000.00"), true);

        when(bankAccountService.listBankAccounts(1L))
                .thenReturn(List.of(account));

        mockMvc.perform(get("/api/bank-accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].bankName")
                        .value("Test Bank"))
                .andExpect(jsonPath("$[0].accountNickname")
                        .value("My Savings Account"))
                .andExpect(jsonPath("$[0].accountIdentifier")
                        .value("DEMO-1234567890"))
                .andExpect(jsonPath("$[0].simulatedBalance")
                        .value(5000.00))
                .andExpect(jsonPath("$[0].isActive").value(true));

        verify(bankAccountService).listBankAccounts(1L);
    }

    @Test
    void getBankAccount_Success() throws Exception {
        SimulatedBankAccount account = createAccount(
                1L, new BigDecimal("5000.00"), true);

        when(bankAccountService.getBankAccount(1L, 1L))
                .thenReturn(account);

        mockMvc.perform(get("/api/bank-accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bankName")
                        .value("Test Bank"))
                .andExpect(jsonPath("$.accountIdentifier")
                        .value("DEMO-1234567890"))
                .andExpect(jsonPath("$.simulatedBalance")
                        .value(5000.00));

        verify(bankAccountService).getBankAccount(1L, 1L);
    }

    @Test
    void deactivateBankAccount_Success() throws Exception {
        SimulatedBankAccount account = createAccount(
                1L, new BigDecimal("5000.00"), false);

        when(bankAccountService.deactivateBankAccount(1L, 1L))
                .thenReturn(account);

        mockMvc.perform(post("/api/bank-accounts/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.isActive").value(false));

        verify(bankAccountService)
                .deactivateBankAccount(1L, 1L);
    }

    @Test
    void addDemoFunds_Success() throws Exception {
        AddDemoFundsRequest request =
                new AddDemoFundsRequest(new BigDecimal("2000.00"));

        SimulatedBankAccount account = createAccount(
                1L, new BigDecimal("7000.00"), true);

        when(bankAccountService.addDemoFunds(
                anyLong(), anyLong(), any(BigDecimal.class)))
                .thenReturn(account);

        mockMvc.perform(post("/api/bank-accounts/1/demo-funds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.simulatedBalance")
                        .value(7000.00));

        verify(bankAccountService)
                .addDemoFunds(1L, 1L, new BigDecimal("2000.00"));
    }
}