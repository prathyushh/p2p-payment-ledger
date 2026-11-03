package com.p2ppayment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2ppayment.dto.*;
import com.p2ppayment.service.WalletService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
@AutoConfigureMockMvc(addFilters = false)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WalletService walletService;

    private WalletResponse walletResponse;
    private TopUpRequest topUpRequest;
    private WalletTopUpResponse topUpResponse;

    @BeforeEach
    void setUp() {
        walletResponse = new WalletResponse(
            1L,
            1L,
            new BigDecimal("5000.00"),
            "ACTIVE",
            new BigDecimal("1000.00"),
            null,
            null
        );

        topUpRequest = new TopUpRequest(1L, new BigDecimal("2000.00"), "1234");

        topUpResponse = new WalletTopUpResponse(
            1L,
            new BigDecimal("5000.00"),
            new BigDecimal("7000.00"),
            new BigDecimal("2000.00"),
            "Top-up successful"
        );
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void getWallet_Success() throws Exception {
        // Arrange
        when(walletService.getWalletByUserId(anyLong())).thenReturn(walletResponse);

        // Act & Assert
        mockMvc.perform(get("/api/wallet")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.balance").value(5000.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void topUp_Success() throws Exception {
        // Arrange
        when(walletService.topUp(anyLong(), anyLong(), any(BigDecimal.class), anyString()))
            .thenReturn(topUpResponse);

        // Act & Assert
        mockMvc.perform(post("/api/wallet/topup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(topUpRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(1))
                .andExpect(jsonPath("$.oldBalance").value(5000.00))
                .andExpect(jsonPath("$.newBalance").value(7000.00))
                .andExpect(jsonPath("$.amount").value(2000.00));
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void topUp_InvalidAmount_ReturnsBadRequest() throws Exception {
        // Arrange
        TopUpRequest invalidRequest = new TopUpRequest(1L, BigDecimal.ZERO, "1234");

        // Act & Assert
        mockMvc.perform(post("/api/wallet/topup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void topUp_MissingPin_ReturnsBadRequest() throws Exception {
        // Arrange
        TopUpRequest invalidRequest = new TopUpRequest(1L, new BigDecimal("2000.00"), null);

        // Act & Assert
        mockMvc.perform(post("/api/wallet/topup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getWallet_Unauthenticated_ReturnsUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/wallet")
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
