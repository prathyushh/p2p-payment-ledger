package com.p2ppayment.controller;

import com.p2ppayment.dto.ScheduledJobLogResponse;
import com.p2ppayment.service.AdminJobService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminJobController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminJobService adminJobService;

    private ScheduledJobLogResponse jobLogResponse;

    @BeforeEach
    void setUp() {
        jobLogResponse = new ScheduledJobLogResponse(
            1L,
            "SETTLEMENT_PROCESSOR",
            "SUCCESS",
            "Processed 15 transactions",
            LocalDateTime.now(),
            LocalDateTime.now().plusSeconds(5),
            5L
        );
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void listJobLogs_Success() throws Exception {
        // Arrange
        Page<ScheduledJobLogResponse> page = new PageImpl<>(Arrays.asList(jobLogResponse));
        when(adminJobService.listJobLogs(any(), anyString(), anyString()))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/jobs/logs")
                .param("page", "0")
                .param("size", "20")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].jobName").value("SETTLEMENT_PROCESSOR"))
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"));
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void listJobLogs_FilterByJobName_Success() throws Exception {
        // Arrange
        Page<ScheduledJobLogResponse> page = new PageImpl<>(Arrays.asList(jobLogResponse));
        when(adminJobService.listJobLogs(any(), anyString(), anyString()))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/jobs/logs")
                .param("page", "0")
                .param("size", "20")
                .param("jobName", "SETTLEMENT_PROCESSOR")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].jobName").value("SETTLEMENT_PROCESSOR"));
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void listJobLogs_FilterByStatus_Success() throws Exception {
        // Arrange
        Page<ScheduledJobLogResponse> page = new PageImpl<>(Arrays.asList(jobLogResponse));
        when(adminJobService.listJobLogs(any(), anyString(), anyString()))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/jobs/logs")
                .param("page", "0")
                .param("size", "20")
                .param("status", "SUCCESS")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"));
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void listJobLogs_SortByStartedAt_Success() throws Exception {
        // Arrange
        Page<ScheduledJobLogResponse> page = new PageImpl<>(Arrays.asList(jobLogResponse));
        when(adminJobService.listJobLogs(any(), anyString(), anyString()))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/jobs/logs")
                .param("page", "0")
                .param("size", "20")
                .param("sortBy", "startedAt")
                .param("sortDir", "desc")
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void listJobLogs_NonAdmin_ReturnsForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/jobs/logs")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
