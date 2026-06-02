package com.servicehub.dto;

import com.servicehub.model.enums.Priority;
import com.servicehub.model.enums.RequestCategory;
import com.servicehub.validation.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ServiceRequestDto {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Category is required")
     @ValidEnum(enumClass = RequestCategory.class, message = "Category must be IT_SUPPORT, FACILITIES, or HR_REQUEST")
    private String category;

    @NotNull(message = "Priority is required")
    @ValidEnum(enumClass = Priority.class, message = "Priority must be LOW, MEDIUM, HIGH, or CRITICAL")
    private String priority;
}
