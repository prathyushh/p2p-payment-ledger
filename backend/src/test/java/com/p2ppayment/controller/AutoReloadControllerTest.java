
package com.p2ppayment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2ppayment.dto.AutoReloadRequest;
import com.p2ppayment.entity.AutoReloadSettings;
import com.p2ppayment.entity.SimulatedBankAccount;
import com.p2ppayment.entity.Wallet;
import com.p2ppayment.service.AutoReloadService;

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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AutoReloadController.class)
@AutoConfigureMockMvc(addFilters = false)
class AutoReloadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AutoReloadService autoReloadService;

    private AutoReloadRequest autoReloadRequest;
    private AutoReloadSettings autoReloadSettings;

    @BeforeEach
    void setUp() {
        autoReloadRequest = new AutoReloadRequest(
                true,
                1L,
                new BigDecimal("500.00"),
                new BigDecimal("2000.00")
        );

        Wallet wallet = Wallet.builder()
                .id(1L)
                .build();

        SimulatedBankAccount account = SimulatedBankAccount.builder()
                .id(1L)
                .accountNickname("Test Bank Account")
                .build();

        LocalDateTime now = LocalDateTime.now();

        autoReloadSettings = AutoReloadSettings.builder()
                .id(1L)
                .wallet(wallet)
                .isEnabled(true)
                .simulatedAccount(account)
                .thresholdAmount(new BigDecimal("500.00"))
                .reloadAmount(new BigDecimal("2000.00"))
                .reloadInProgress(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        setAuthenticatedUser();
    }

    private void setAuthenticatedUser() {
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

    @Test
    void getAutoReloadSettings_Success() throws Exception {
        when(autoReloadService.getSettings(1L))
                .thenReturn(autoReloadSettings);

        mockMvc.perform(get("/api/auto-reload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.walletId").value(1))
                .andExpect(jsonPath("$.isEnabled").value(true))
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.accountNickname")
                        .value("Test Bank Account"))
                .andExpect(jsonPath("$.thresholdAmount").value(500.00))
                .andExpect(jsonPath("$.reloadAmount").value(2000.00));

        verify(autoReloadService).getSettings(1L);
    }

    @Test
    void updateAutoReloadSettings_Success() throws Exception {
        when(autoReloadService.updateSettings(
                1L,
                true,
                1L,
                new BigDecimal("500.00"),
                new BigDecimal("2000.00")
        )).thenReturn(autoReloadSettings);

        doNothing().when(autoReloadService)
                .attemptReload(1L, false);

        mockMvc.perform(put("/api/auto-reload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                autoReloadRequest
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.walletId").value(1))
                .andExpect(jsonPath("$.isEnabled").value(true))
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.accountNickname")
                        .value("Test Bank Account"))
                .andExpect(jsonPath("$.thresholdAmount").value(500.00))
                .andExpect(jsonPath("$.reloadAmount").value(2000.00));

        verify(autoReloadService).updateSettings(
                1L,
                true,
                1L,
                new BigDecimal("500.00"),
                new BigDecimal("2000.00")
        );

        verify(autoReloadService).attemptReload(1L, false);
    }

    @Test
    void updateAutoReloadSettings_InvalidThreshold_ReturnsBadRequest()
            throws Exception {

        AutoReloadRequest invalidRequest = new AutoReloadRequest(
                true,
                1L,
                BigDecimal.ZERO,
                new BigDecimal("2000.00")
        );

        mockMvc.perform(put("/api/auto-reload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                invalidRequest
                        )))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAutoReloadSettings_InvalidReloadAmount_ReturnsBadRequest()
            throws Exception {

        AutoReloadRequest invalidRequest = new AutoReloadRequest(
                true,
                1L,
                new BigDecimal("500.00"),
                BigDecimal.ZERO
        );

        mockMvc.perform(put("/api/auto-reload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                invalidRequest
                        )))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAutoReloadSettings_InvalidUserId_ReturnsUnauthorized()
            throws Exception {

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "invalid-user-id",
                        null,
                        Collections.emptyList()
                )
        );

        SecurityContextHolder.setContext(context);

        mockMvc.perform(get("/api/auto-reload"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAutoReloadSettings_Unauthenticated_ReturnsUnauthorized()
            throws Exception {

        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/auto-reload"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAutoReloadSettings_WithoutSimulatedAccount_Success()
            throws Exception {

        AutoReloadSettings settingsWithoutAccount =
                AutoReloadSettings.builder()
                        .id(2L)
                        .wallet(Wallet.builder().id(1L).build())
                        .isEnabled(false)
                        .simulatedAccount(null)
                        .thresholdAmount(new BigDecimal("500.00"))
                        .reloadAmount(new BigDecimal("2000.00"))
                        .reloadInProgress(false)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

        when(autoReloadService.getSettings(1L))
                .thenReturn(settingsWithoutAccount);

        mockMvc.perform(get("/api/auto-reload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.accountId").value(
                        org.hamcrest.Matchers.nullValue()
                ))
                .andExpect(jsonPath("$.accountNickname").value(
                        org.hamcrest.Matchers.nullValue()
                ));
    }
}
