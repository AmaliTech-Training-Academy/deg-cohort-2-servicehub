package com.servicehub.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
public class AuthRequest {
    @NotBlank @Email
    private String email;
    @NotBlank
    private String password;
}
