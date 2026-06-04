package com.servicehub.dto;

import jakarta.validation.constraints.Min;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class SlaPolicyUpdateDto {

    @Min(0)
    private Integer responseTimeHours;

    @Min(1)
    private Integer resolutionTimeHours;
}
