package com.servicehub.dto;

import com.servicehub.model.enums.Priority;
import com.servicehub.model.enums.RequestCategory;
import com.servicehub.validation.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateRequestDto {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @ValidEnum(enumClass = RequestCategory.class, message = "Category must be IT_SUPPORT, FACILITIES, or HR_REQUEST")
    private String category;

    @ValidEnum(enumClass = Priority.class, message = "Priority must be LOW, MEDIUM, HIGH, or CRITICAL")
    private String priority;
}
