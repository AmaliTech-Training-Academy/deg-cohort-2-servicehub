package com.servicehub.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ServiceRequestResponse {
    private Long id;
    private String title;
    private String description;
    private String category;
    private String priority;
    private String status;
    private String departmentName;
    private String assignedToName;
    private String requesterName;
    private LocalDateTime slaDeadline;
    private LocalDateTime responseDeadline;
    private LocalDateTime firstResponseAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private Boolean isOverdue;
    private Boolean isResponseOverdue;
    private String slaStatus;
    private Long responseTimeMinutes;
    private Long resolutionTimeMinutes;
    private Boolean slaBreached;
    private List<StatusTransitionLogResponse> transitionHistory;
}
