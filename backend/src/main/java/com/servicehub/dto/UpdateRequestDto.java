package com.servicehub.dto;

import lombok.Data;

@Data
public class UpdateRequestDto {
    private String title;
    private String description;
    private String category;
    private String priority;
}
