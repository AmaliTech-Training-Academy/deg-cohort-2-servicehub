package com.servicehub.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
public class ServiceRequestDto {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Category is required")
    @Pattern(regexp = "IT_SUPPORT|FACILITIES|HR_REQUEST",
             message = "Category must be IT_SUPPORT, FACILITIES, or HR_REQUEST")
    private String category;

    @NotNull(message = "Priority is required")
    @Pattern(regexp = "LOW|MEDIUM|HIGH|CRITICAL",
             message = "Priority must be LOW, MEDIUM, HIGH, or CRITICAL")
    private String priority;
}
