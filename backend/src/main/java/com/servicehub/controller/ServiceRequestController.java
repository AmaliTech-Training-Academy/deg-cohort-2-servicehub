package com.servicehub.controller;

import com.servicehub.dto.*;
import com.servicehub.service.ServiceRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class ServiceRequestController {
    private final ServiceRequestService requestService;

    @GetMapping
    public ResponseEntity<Page<ServiceRequestResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(requestService.getAllRequests(page, size));
    }

    @GetMapping("/my-requests")
    public ResponseEntity<Page<ServiceRequestResponse>> getMyRequests(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(requestService.getMyRequests(email, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceRequestResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.getRequestById(id));
    }

    @PostMapping
    public ResponseEntity<ServiceRequestResponse> create(
            @Valid @RequestBody ServiceRequestDto dto,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(requestService.createRequest(dto, email));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceRequestResponse> update(
            @PathVariable Long id,
            @RequestBody UpdateRequestDto dto,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(requestService.updateRequest(id, dto, email));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ServiceRequestResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(requestService.updateStatus(id, request, email));
    }
}
