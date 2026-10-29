package com.p2ppayment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScheduledJobLogResponse {
    private Long id;
    private String jobName;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String status;
    private Integer recordsProcessed;
    private Integer recordsFailed;
    private String totalAmount;
    private String errorMessage;
}