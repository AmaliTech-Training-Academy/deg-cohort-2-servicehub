package com.servicehub.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Comment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private ServiceRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 1000)
    private String body;

    @Column(name = "system_generated", nullable = false)
    private boolean systemGenerated;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
