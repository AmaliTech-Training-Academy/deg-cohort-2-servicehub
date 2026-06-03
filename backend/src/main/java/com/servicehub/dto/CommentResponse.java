package com.servicehub.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CommentResponse {
    private Long id;
    private String authorName;
    private String body;
    private boolean systemGenerated;
    private LocalDateTime createdAt;
}
