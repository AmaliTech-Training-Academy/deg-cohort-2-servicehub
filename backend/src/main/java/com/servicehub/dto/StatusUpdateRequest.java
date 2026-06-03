package com.servicehub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
public class StatusUpdateRequest {
    @NotBlank(message = "newStatus must not be blank")
    private String newStatus;
    private String comment;
}
