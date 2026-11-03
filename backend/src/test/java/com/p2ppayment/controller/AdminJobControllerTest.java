
package com.p2ppayment.controller;

import com.p2ppayment.entity.ScheduledJobLog;
import com.p2ppayment.repository.ScheduledJobLogRepository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminJobController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScheduledJobLogRepository scheduledJobLogRepository;

    private ScheduledJobLog createJobLog() {
        ScheduledJobLog jobLog = mock(ScheduledJobLog.class);

        when(jobLog.getId()).thenReturn(1L);
        when(jobLog.getJobName()).thenReturn("SETTLEMENT_PROCESSOR");
        when(jobLog.getStatus()).thenReturn(
                ScheduledJobLog.Status.SUCCESS
        );
        when(jobLog.getStartedAt()).thenReturn(LocalDateTime.now());
        when(jobLog.getEndedAt()).thenReturn(LocalDateTime.now());
        when(jobLog.getRecordsProcessed()).thenReturn(15);
        when(jobLog.getRecordsFailed()).thenReturn(0);
        when(jobLog.getTotalAmount()).thenReturn(null);
        when(jobLog.getErrorMessage()).thenReturn(null);

        return jobLog;
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void listJobLogs_Success() throws Exception {
        ScheduledJobLog jobLog = createJobLog();

        Page<ScheduledJobLog> page =
                new PageImpl<>(Collections.singletonList(jobLog));

        when(scheduledJobLogRepository.findAll(any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].jobName")
                        .value("SETTLEMENT_PROCESSOR"))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$[0].recordsProcessed").value(15))
                .andExpect(jsonPath("$[0].recordsFailed").value(0))
                .andExpect(jsonPath("$[0].totalAmount").doesNotExist());

        verify(scheduledJobLogRepository).findAll(any(Pageable.class));
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void listJobLogs_EmptyList() throws Exception {
        when(scheduledJobLogRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/admin/jobs"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(scheduledJobLogRepository).findAll(any(Pageable.class));
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void getJobLog_Success() throws Exception {
        ScheduledJobLog jobLog = createJobLog();

        when(scheduledJobLogRepository.findById(1L))
                .thenReturn(Optional.of(jobLog));

        mockMvc.perform(get("/api/admin/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.jobName")
                        .value("SETTLEMENT_PROCESSOR"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(scheduledJobLogRepository).findById(1L);
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void getJobLog_NotFound() throws Exception {
        when(scheduledJobLogRepository.findById(99L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/jobs/99"))
                .andExpect(status().isNotFound());

        verify(scheduledJobLogRepository).findById(99L);
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void getJobLog_WithTotalAmount_Success() throws Exception {
        ScheduledJobLog jobLog = createJobLog();

        // Override the null stub to cover the non-null totalAmount branch.
        when(jobLog.getTotalAmount())
                .thenReturn(new BigDecimal("15000.00"));

        when(scheduledJobLogRepository.findById(1L))
                .thenReturn(Optional.of(jobLog));

        mockMvc.perform(get("/api/admin/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.jobName")
                        .value("SETTLEMENT_PROCESSOR"))
                .andExpect(jsonPath("$.totalAmount").value("15000.00"));

        verify(scheduledJobLogRepository).findById(1L);
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void listJobLogs_NonAdmin() throws Exception {
        // Filters are disabled, so this test does not verify role enforcement.
        when(scheduledJobLogRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/admin/jobs"))
                .andExpect(status().isOk());
    }
}
