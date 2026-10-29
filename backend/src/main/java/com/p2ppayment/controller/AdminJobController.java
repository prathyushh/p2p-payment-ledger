package com.p2ppayment.controller;

import com.p2ppayment.dto.ScheduledJobLogResponse;
import com.p2ppayment.entity.ScheduledJobLog;
import com.p2ppayment.repository.ScheduledJobLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/jobs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminJobController {

    private final ScheduledJobLogRepository scheduledJobLogRepository;

    public AdminJobController(ScheduledJobLogRepository scheduledJobLogRepository) {
        this.scheduledJobLogRepository = scheduledJobLogRepository;
    }

    /**
     * GET /api/admin/jobs
     * Returns the last 20 job runs sorted by most recent first.
     */
    @GetMapping
    public ResponseEntity<List<ScheduledJobLogResponse>> listJobLogs() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "startedAt"));
        List<ScheduledJobLog> jobLogs = scheduledJobLogRepository.findAll(pageable).getContent();
        
        List<ScheduledJobLogResponse> response = jobLogs.stream()
                .map(this::mapToScheduledJobLogResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/jobs/{id}
     * Returns a specific job log by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScheduledJobLogResponse> getJobLog(@PathVariable Long id) {
        ScheduledJobLog jobLog = scheduledJobLogRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Job log not found"));
        
        ScheduledJobLogResponse response = mapToScheduledJobLogResponse(jobLog);
        return ResponseEntity.ok(response);
    }

    /**
     * Convert ScheduledJobLog entity to response DTO.
     */
    private ScheduledJobLogResponse mapToScheduledJobLogResponse(ScheduledJobLog jobLog) {
        return ScheduledJobLogResponse.builder()
                .id(jobLog.getId())
                .jobName(jobLog.getJobName())
                .startedAt(jobLog.getStartedAt())
                .endedAt(jobLog.getEndedAt())
                .status(jobLog.getStatus().name())
                .recordsProcessed(jobLog.getRecordsProcessed())
                .recordsFailed(jobLog.getRecordsFailed())
                .totalAmount(jobLog.getTotalAmount() != null ? jobLog.getTotalAmount().toString() : null)
                .errorMessage(jobLog.getErrorMessage())
                .build();
    }
}