
package com.p2ppayment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2ppayment.entity.Notification;
import com.p2ppayment.entity.Transaction;
import com.p2ppayment.service.NotificationService;
import com.p2ppayment.util.JwtTokenProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
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

    private Notification createNotification(
            Long id,
            Transaction relatedTransaction) {

        return Notification.builder()
                .id(id)
                .type("TRANSFER_INITIATED")
                .title("Transfer Initiated")
                .message("Your transfer of Rs 1,000 has been initiated")
                .isRead(false)
                .relatedTransaction(relatedTransaction)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getNotifications_Success() throws Exception {
        Notification notification = createNotification(1L, null);

        Page<Notification> page =
                new PageImpl<>(List.of(notification));

        when(notificationService.getNotifications(eq(1L), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/notifications")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].type")
                        .value("TRANSFER_INITIATED"))
                .andExpect(jsonPath("$.content[0].title")
                        .value("Transfer Initiated"))
                .andExpect(jsonPath("$.content[0].message")
                        .value("Your transfer of Rs 1,000 has been initiated"))
                .andExpect(jsonPath("$.content[0].isRead").value(false));

        verify(notificationService).getNotifications(eq(1L), any());
    }

    @Test
    void getNotifications_WithRelatedTransaction_Success()
            throws Exception {

        Transaction transaction = mock(Transaction.class);
        when(transaction.getId()).thenReturn(100L);

        Notification notification = createNotification(2L, transaction);

        Page<Notification> page =
                new PageImpl<>(List.of(notification));

        when(notificationService.getNotifications(eq(1L), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.content[0].relatedTransactionId")
                        .value(100));

        verify(notificationService).getNotifications(eq(1L), any());
    }

    @Test
    void getNotifications_DefaultPagination_Success()
            throws Exception {

        when(notificationService.getNotifications(eq(1L), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(notificationService).getNotifications(eq(1L), any());
    }

    @Test
    void getUnreadCount_Success() throws Exception {
        when(notificationService.getUnreadCount(1L))
                .thenReturn(5L);

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(5));

        verify(notificationService).getUnreadCount(1L);
    }

    @Test
    void markAsRead_Success() throws Exception {
        when(notificationService.markAsRead(1L, 1L))
                .thenReturn(true);

        mockMvc.perform(put("/api/notifications/1/read"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(notificationService).markAsRead(1L, 1L);
    }

    @Test
    void markAsRead_NotFound() throws Exception {
        when(notificationService.markAsRead(999L, 1L))
                .thenReturn(false);

        mockMvc.perform(put("/api/notifications/999/read"))
                .andExpect(status().isNotFound());

        verify(notificationService).markAsRead(999L, 1L);
    }

    @Test
    void markAllAsRead_Success() throws Exception {
        when(notificationService.markAllAsRead(1L))
                .thenReturn(3);

        mockMvc.perform(put("/api/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markedAsReadCount").value(3));

        verify(notificationService).markAllAsRead(1L);
    }
}