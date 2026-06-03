package com.servicehub.model;

import com.servicehub.model.enums.Priority;
import com.servicehub.model.enums.RequestCategory;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sla_policies", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"category", "priority"})
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SlaPolicy {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private RequestCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Column(nullable = false)
    private Integer responseTimeHours;

    @Column(nullable = false)
    private Integer resolutionTimeHours;
}
