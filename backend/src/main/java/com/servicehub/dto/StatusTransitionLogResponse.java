package com.servicehub.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StatusTransitionLogResponse {
    private String fromStatus;
    private String toStatus;
    private String changedByName;
    private String comment;
    private LocalDateTime changedAt;
}
