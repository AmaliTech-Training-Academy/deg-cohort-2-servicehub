package com.servicehub.controller;

import com.servicehub.dto.ServiceRequestResponse;
import com.servicehub.dto.SlaPolicyUpdateDto;
import com.servicehub.model.SlaPolicy;
import com.servicehub.service.ServiceRequestService;
import com.servicehub.service.SlaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sla")
@RequiredArgsConstructor
@Tag(name = "SLA", description = "SLA breach detection and policy management")
public class SlaController {

    private final SlaService slaService;
    private final ServiceRequestService requestService;

    @GetMapping("/breaches")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "List all SLA-breached requests (response + resolution)")
    public ResponseEntity<List<ServiceRequestResponse>> getAllBreaches() {
        return ResponseEntity.ok(
                slaService.getAllBreaches().stream().map(requestService::toResponse).toList());
    }

    @GetMapping("/breaches/response")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "List requests that missed their response SLA")
    public ResponseEntity<List<ServiceRequestResponse>> getResponseBreaches() {
        return ResponseEntity.ok(
                slaService.getResponseBreaches().stream().map(requestService::toResponse).toList());
    }

    @GetMapping("/breaches/resolution")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "List requests that missed their resolution SLA")
    public ResponseEntity<List<ServiceRequestResponse>> getResolutionBreaches() {
        return ResponseEntity.ok(
                slaService.getResolutionBreaches().stream().map(requestService::toResponse).toList());
    }

    @GetMapping("/policies")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "List all SLA policies")
    public ResponseEntity<List<SlaPolicy>> getPolicies() {
        return ResponseEntity.ok(slaService.getAllPolicies());
    }

    @PutMapping("/policies/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Update an SLA policy (MANAGER only)")
    public ResponseEntity<SlaPolicy> updatePolicy(
            @PathVariable Long id,
            @Valid @RequestBody SlaPolicyUpdateDto dto,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(slaService.updatePolicy(id, dto, email));
    }
}
