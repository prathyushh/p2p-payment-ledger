package com.p2ppayment.controller;

import com.p2ppayment.dto.NotificationResponse;
import com.p2ppayment.service.NotificationService;
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

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    private NotificationResponse notificationResponse;

    @BeforeEach
    void setUp() {
        notificationResponse = new NotificationResponse(
            1L,
            1L,
            "TRANSFER_INITIATED",
            "Transfer Initiated",
            "Your transfer of Rs 1,000 has been initiated",
            false,
            1L,
            LocalDateTime.now()
        );
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void getNotifications_Success() throws Exception {
        // Arrange
        Page<NotificationResponse> page = new PageImpl<>(Arrays.asList(notificationResponse));
        when(notificationService.getNotifications(anyLong(), any())).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/notifications")
                .param("page", "0")
                .param("size", "20")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Transfer Initiated"))
                .andExpect(jsonPath("$.content[0].isRead").value(false));
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void getUnreadCount_Success() throws Exception {
        // Arrange
        when(notificationService.getUnreadCount(anyLong())).thenReturn(5L);

        // Act & Assert
        mockMvc.perform(get("/api/notifications/unread-count")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void markAsRead_Success() throws Exception {
        // Arrange
        when(notificationService.markAsRead(anyLong(), anyLong())).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/notifications/1/read")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void markAsRead_NotFound_ReturnsFalse() throws Exception {
        // Arrange
        when(notificationService.markAsRead(anyLong(), anyLong())).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/notifications/999/read")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void markAllAsRead_Success() throws Exception {
        // Arrange
        when(notificationService.markAllAsRead(anyLong())).thenReturn(3);

        // Act & Assert
        mockMvc.perform(post("/api/notifications/mark-all-read")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }
}
