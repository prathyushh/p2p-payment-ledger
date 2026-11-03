package com.p2ppayment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2ppayment.dto.*;
import com.p2ppayment.service.AutoReloadService;
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
    private AutoReloadResponse autoReloadResponse;

    @BeforeEach
    void setUp() {
        autoReloadRequest = new AutoReloadRequest(
            true,
            new BigDecimal("500.00"),
            new BigDecimal("2000.00"),
            1L
        );

        autoReloadResponse = new AutoReloadResponse(
            1L,
            1L,
            true,
            new BigDecimal("500.00"),
            new BigDecimal("2000.00"),
            1L,
            "Test Bank Account"
        );
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void getAutoReloadSettings_Success() throws Exception {
        // Arrange
        when(autoReloadService.getSettings(anyLong())).thenReturn(autoReloadResponse);

        // Act & Assert
        mockMvc.perform(get("/api/auto-reload")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.threshold").value(500.00))
                .andExpect(jsonPath("$.reloadAmount").value(2000.00));
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void updateAutoReloadSettings_Success() throws Exception {
        // Arrange
        when(autoReloadService.updateSettings(anyLong(), any(AutoReloadRequest.class)))
            .thenReturn(autoReloadResponse);

        // Act & Assert
        mockMvc.perform(put("/api/auto-reload")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(autoReloadRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.threshold").value(500.00));
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void updateAutoReloadSettings_InvalidThreshold_ReturnsBadRequest() throws Exception {
        // Arrange
        AutoReloadRequest invalidRequest = new AutoReloadRequest(
            true,
            BigDecimal.ZERO,
            new BigDecimal("2000.00"),
            1L
        );

        // Act & Assert
        mockMvc.perform(put("/api/auto-reload")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void updateAutoReloadSettings_InvalidReloadAmount_ReturnsBadRequest() throws Exception {
        // Arrange
        AutoReloadRequest invalidRequest = new AutoReloadRequest(
            true,
            new BigDecimal("500.00"),
            BigDecimal.ZERO,
            1L
        );

        // Act & Assert
        mockMvc.perform(put("/api/auto-reload")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void disableAutoReload_Success() throws Exception {
        // Arrange
        AutoReloadResponse disabledResponse = new AutoReloadResponse(
            1L,
            1L,
            false,
            new BigDecimal("500.00"),
            new BigDecimal("2000.00"),
            1L,
            "Test Bank Account"
        );
        when(autoReloadService.disableAutoReload(anyLong())).thenReturn(disabledResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auto-reload/disable")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }
}
