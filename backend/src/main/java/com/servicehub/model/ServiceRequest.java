package com.servicehub.model;

import com.servicehub.model.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_requests", indexes = {
        @Index(name = "idx_sr_status",       columnList = "status"),
        @Index(name = "idx_sr_category",     columnList = "category"),
        @Index(name = "idx_sr_requester",    columnList = "requester_id"),
        @Index(name = "idx_sr_assigned",     columnList = "assigned_to_id"),
        @Index(name = "idx_sr_sla_deadline", columnList = "sla_deadline"),
        @Index(name = "idx_sr_created_at",   columnList = "created_at")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceRequest {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(name = "sla_deadline")
    private LocalDateTime slaDeadline;

    @Column(name = "response_deadline")
    private LocalDateTime responseDeadline;

    @Column(name = "first_response_at")
    private LocalDateTime firstResponseAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    @Builder.Default
    @Column(name = "sla_breached", nullable = false)
    private boolean slaBreached = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
