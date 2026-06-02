package com.servicehub.controller;

import com.servicehub.dto.DashboardStatsResponse;
import com.servicehub.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Aggregate metrics and SLA reporting")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('MANAGER','AGENT')")
    @Operation(summary = "Overall dashboard stats")
    @ApiResponse(responseCode = "200", description = "Stats returned")
    @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    public ResponseEntity<DashboardStatsResponse> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    @GetMapping("/sla")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "SLA compliance rate per category")
    @ApiResponse(responseCode = "200", description = "Compliance map returned")
    @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    public ResponseEntity<Map<String, Double>> getSla() {
        return ResponseEntity.ok(dashboardService.getSlaStats());
    }

    @GetMapping("/trends")
    @PreAuthorize("hasAnyRole('MANAGER','AGENT')")
    @Operation(summary = "Daily request volume for the last N days")
    @ApiResponse(responseCode = "200", description = "Trend map returned")
    public ResponseEntity<Map<String, Long>> getTrends(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(dashboardService.getDailyVolumeTrend(days));
    }
}
