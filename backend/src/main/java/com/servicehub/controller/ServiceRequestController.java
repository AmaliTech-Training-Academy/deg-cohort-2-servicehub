package com.servicehub.controller;

import com.servicehub.dto.*;
import com.servicehub.service.ServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
@Tag(name = "Service Requests", description = "Create and manage service requests")
public class ServiceRequestController {
    private final ServiceRequestService requestService;

    @GetMapping
    @Operation(summary = "List all requests (paginated)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of service requests"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                content = @Content(schema = @Schema(example = "{\"error\": \"Unauthorized: missing or invalid token\"}")))
    })
    public ResponseEntity<Page<ServiceRequestResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(requestService.getAllRequests(page, size));
    }

    @GetMapping("/my-requests")
    @Operation(summary = "List requests submitted by the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of the caller's own requests"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                content = @Content(schema = @Schema(example = "{\"error\": \"Unauthorized: missing or invalid token\"}")))
    })
    public ResponseEntity<Page<ServiceRequestResponse>> getMyRequests(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(requestService.getMyRequests(email, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single request by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Request found"),
        @ApiResponse(responseCode = "400", description = "Request not found",
                content = @Content(schema = @Schema(example = "{\"error\": \"Request not found\"}"))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                content = @Content(schema = @Schema(example = "{\"error\": \"Unauthorized: missing or invalid token\"}")))
    })
    public ResponseEntity<ServiceRequestResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.getRequestById(id));
    }

    @PostMapping
    @Operation(summary = "Submit a new service request")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Request created — department auto-assigned, SLA deadline set"),
        @ApiResponse(responseCode = "400", description = "Validation error",
                content = @Content(schema = @Schema(example = "{\"title\": \"must not be blank\"}"))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                content = @Content(schema = @Schema(example = "{\"error\": \"Unauthorized: missing or invalid token\"}")))
    })
    public ResponseEntity<ServiceRequestResponse> create(
            @Valid @RequestBody ServiceRequestDto dto,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(requestService.createRequest(dto, email));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a request (requester or MANAGER only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Request updated"),
        @ApiResponse(responseCode = "400", description = "Request not found or invalid data",
                content = @Content(schema = @Schema(example = "{\"error\": \"Request not found\"}"))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                content = @Content(schema = @Schema(example = "{\"error\": \"Unauthorized: missing or invalid token\"}"))),
        @ApiResponse(responseCode = "403", description = "Only the original requester or a MANAGER can edit",
                content = @Content(schema = @Schema(example = "{\"error\": \"Forbidden: insufficient permissions for this action\"}")))
    })
    public ResponseEntity<ServiceRequestResponse> update(
            @PathVariable Long id,
            @RequestBody UpdateRequestDto dto,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(requestService.updateRequest(id, dto, email));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Advance request status (AGENT or MANAGER only)",
            description = "Valid transitions: OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated"),
        @ApiResponse(responseCode = "400", description = "Invalid status transition or request not found",
                content = @Content(schema = @Schema(example = "{\"error\": \"Invalid status transition: OPEN -> IN_PROGRESS\"}"))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                content = @Content(schema = @Schema(example = "{\"error\": \"Unauthorized: missing or invalid token\"}"))),
        @ApiResponse(responseCode = "403", description = "Only AGENT or MANAGER can update status",
                content = @Content(schema = @Schema(example = "{\"error\": \"Forbidden: insufficient permissions for this action\"}")))
    })
    public ResponseEntity<ServiceRequestResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(requestService.updateStatus(id, request, email));
    }
}
