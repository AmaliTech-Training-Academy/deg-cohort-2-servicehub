package com.servicehub.controller;

import com.servicehub.dto.*;
import com.servicehub.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register and login to receive a JWT token")
@SecurityRequirements
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registered successfully — returns JWT token",
                content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error or email already in use",
                content = @Content(schema = @Schema(example = "{\"error\": \"Email already registered\"}")))
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and get a JWT token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful — copy the token and use it in Authorize",
                content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid credentials",
                content = @Content(schema = @Schema(example = "{\"error\": \"Invalid credentials\"}")))
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
