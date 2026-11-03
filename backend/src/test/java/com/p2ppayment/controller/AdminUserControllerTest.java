package com.p2ppayment.controller;

import com.p2ppayment.dto.AdminUserResponse;
import com.p2ppayment.service.AdminUserService;
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

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;

    private AdminUserResponse adminUserResponse;

    @BeforeEach
    void setUp() {
        adminUserResponse = new AdminUserResponse(
            1L,
            "test@example.com",
            "Test User",
            "USER",
            true,
            false,
            null,
            0,
            2L,
            new BigDecimal("5000.00"),
            "ACTIVE",
            LocalDateTime.now()
        );
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void listUsers_Success() throws Exception {
        // Arrange
        Page<AdminUserResponse> page = new PageImpl<>(Arrays.asList(adminUserResponse));
        when(adminUserService.listUsers(any(), anyString(), anyString(), any()))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/users")
                .param("page", "0")
                .param("size", "20")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].email").value("test@example.com"))
                .andExpect(jsonPath("$.content[0].role").value("USER"));
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void getUserDetails_Success() throws Exception {
        // Arrange
        when(adminUserService.getUserDetails(anyLong())).thenReturn(adminUserResponse);

        // Act & Assert
        mockMvc.perform(get("/api/admin/users/1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.walletBalance").value(5000.00))
                .andExpect(jsonPath("$.walletStatus").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void listUsers_WithFilters_Success() throws Exception {
        // Arrange
        Page<AdminUserResponse> page = new PageImpl<>(Arrays.asList(adminUserResponse));
        when(adminUserService.listUsers(any(), anyString(), anyString(), any()))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/users")
                .param("page", "0")
                .param("size", "20")
                .param("role", "USER")
                .param("status", "ACTIVE")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void listUsers_NonAdmin_ReturnsForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/users")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
