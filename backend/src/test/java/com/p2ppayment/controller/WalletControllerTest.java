package com.p2ppayment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2ppayment.dto.AdminWalletRequest;
import com.p2ppayment.dto.WalletTopUpRequest;
import com.p2ppayment.entity.Wallet;
import com.p2ppayment.service.WalletService;

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
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
@AutoConfigureMockMvc(addFilters = false)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WalletService walletService;

    @BeforeEach
    void setUp() {
        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "1",
                        null,
                        Collections.emptyList()
                )
        );

        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private Wallet mockWallet(
            Long id,
            String balance,
            String dailySpent,
            String dailyLimit,
            Wallet.Status walletStatus) {

        Wallet wallet = mock(Wallet.class);

        when(wallet.getId()).thenReturn(id);
        when(wallet.getBalance()).thenReturn(new BigDecimal(balance));
        when(wallet.getDailySpent()).thenReturn(new BigDecimal(dailySpent));
        when(wallet.getDailyLimit()).thenReturn(new BigDecimal(dailyLimit));
        when(wallet.getStatus()).thenReturn(walletStatus);

        return wallet;
    }

    // GET WALLET

    @Test
    void getWallet_Success() throws Exception {
        Wallet wallet = mockWallet(
                1L, "5000.00", "1000.00", "50000.00",
                Wallet.Status.ACTIVE
        );

        when(walletService.getWalletByUserId(1L))
                .thenReturn(wallet);

        mockMvc.perform(get("/api/wallet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.balance").value(5000.00))
                .andExpect(jsonPath("$.dailySpent").value(1000.00))
                .andExpect(jsonPath("$.dailyLimit").value(50000.00))
                .andExpect(jsonPath("$.dailyRemaining").value(49000.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(walletService).getWalletByUserId(1L);
    }

    // TOP-UP WALLET

    @Test
    void topUp_Success() throws Exception {
        WalletTopUpRequest request = new WalletTopUpRequest(
                10L, new BigDecimal("2000.00")
        );

        Wallet wallet = mockWallet(
                1L, "7000.00", "1000.00", "50000.00",
                Wallet.Status.ACTIVE
        );

        when(walletService.topUp(
                10L, 1L, new BigDecimal("2000.00")
        )).thenReturn(wallet);

        mockMvc.perform(post("/api/wallet/top-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(1))
                .andExpect(jsonPath("$.amount").value(2000.00))
                .andExpect(jsonPath("$.newBalance").value(7000.00))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message")
                        .value("Wallet top-up completed successfully"));

        verify(walletService).topUp(
                10L, 1L, new BigDecimal("2000.00")
        );
    }

    @Test
    void topUp_InvalidAmount_ReturnsBadRequest() throws Exception {
        WalletTopUpRequest request = new WalletTopUpRequest(
                10L, BigDecimal.ZERO
        );

        mockMvc.perform(post("/api/wallet/top-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void topUp_MissingAccountId_ReturnsBadRequest() throws Exception {
        WalletTopUpRequest request = new WalletTopUpRequest(
                null, new BigDecimal("2000.00")
        );

        mockMvc.perform(post("/api/wallet/top-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // DEACTIVATE WALLET

    @Test
    void deactivateWallet_Success() throws Exception {
        Wallet wallet = mockWallet(
                1L, "5000.00", "1000.00", "50000.00",
                Wallet.Status.INACTIVE
        );

        when(walletService.deactivateWallet(1L, "127.0.0.1"))
                .thenReturn(wallet);

        mockMvc.perform(post("/api/wallet/deactivate")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        verify(walletService).deactivateWallet(1L, "127.0.0.1");
    }

    @Test
    void deactivateWallet_UsesForwardedIpAddress() throws Exception {
        Wallet wallet = mockWallet(
                1L, "5000.00", "1000.00", "50000.00",
                Wallet.Status.INACTIVE
        );

        when(walletService.deactivateWallet(1L, "203.0.113.10"))
                .thenReturn(wallet);

        mockMvc.perform(post("/api/wallet/deactivate")
                        .header("X-Forwarded-For",
                                "203.0.113.10, 10.0.0.1"))
                .andExpect(status().isOk());

        verify(walletService).deactivateWallet(1L, "203.0.113.10");
    }

    @Test
    void deactivateWallet_UsesRealIpHeader() throws Exception {
        Wallet wallet = mockWallet(
                1L, "5000.00", "1000.00", "50000.00",
                Wallet.Status.INACTIVE
        );

        when(walletService.deactivateWallet(1L, "203.0.113.20"))
                .thenReturn(wallet);

        mockMvc.perform(post("/api/wallet/deactivate")
                        .header("X-Real-IP", "203.0.113.20"))
                .andExpect(status().isOk());

        verify(walletService).deactivateWallet(1L, "203.0.113.20");
    }

    // REACTIVATE WALLET

    @Test
    void reactivateWallet_Success() throws Exception {
        Wallet wallet = mockWallet(
                1L, "5000.00", "1000.00", "50000.00",
                Wallet.Status.ACTIVE
        );

        when(walletService.reactivateWallet(1L, "127.0.0.1"))
                .thenReturn(wallet);

        mockMvc.perform(post("/api/wallet/reactivate")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(walletService).reactivateWallet(1L, "127.0.0.1");
    }

    // ADMIN FREEZE WALLET

    @Test
    void freezeWallet_Success() throws Exception {
        Wallet wallet = mockWallet(
                10L, "5000.00", "0.00", "50000.00",
                Wallet.Status.FROZEN
        );

        when(walletService.freezeWallet(10L, null, "127.0.0.1"))
                .thenReturn(wallet);

        AdminWalletRequest request =
                new AdminWalletRequest(10L, "Test reason");

        mockMvc.perform(post("/api/wallet/admin/freeze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(httpRequest -> {
                            httpRequest.setRemoteAddr("127.0.0.1");
                            return httpRequest;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("FROZEN"));

        verify(walletService).freezeWallet(10L, null, "127.0.0.1");
    }

    // ADMIN UNFREEZE WALLET

    @Test
    void unfreezeWallet_Success() throws Exception {
        Wallet wallet = mockWallet(
                10L, "5000.00", "0.00", "50000.00",
                Wallet.Status.ACTIVE
        );

        when(walletService.unfreezeWallet(10L, null, "127.0.0.1"))
                .thenReturn(wallet);

        AdminWalletRequest request =
                new AdminWalletRequest(10L, "Test reason");

        mockMvc.perform(post("/api/wallet/admin/unfreeze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(httpRequest -> {
                            httpRequest.setRemoteAddr("127.0.0.1");
                            return httpRequest;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(walletService).unfreezeWallet(10L, null, "127.0.0.1");
    }
}