package com.servicehub.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateRequestDto {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @Pattern(regexp = "IT_SUPPORT|FACILITIES|HR_REQUEST",
             message = "Category must be IT_SUPPORT, FACILITIES, or HR_REQUEST")
    private String category;

    @Pattern(regexp = "LOW|MEDIUM|HIGH|CRITICAL",
             message = "Priority must be LOW, MEDIUM, HIGH, or CRITICAL")
    private String priority;
}
