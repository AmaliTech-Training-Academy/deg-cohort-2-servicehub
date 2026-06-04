package com.servicehub.controller;

import com.servicehub.config.CorsConfig;
import com.servicehub.config.SecurityConfig;
import com.servicehub.dto.DashboardStatsResponse;
import com.servicehub.service.DashboardService;
import com.servicehub.support.JwtTokenFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC-09 RBAC matrix for DashboardController.
 *
 * /stats   — MANAGER + AGENT allowed; EMPLOYEE forbidden
 * /sla     — MANAGER only
 * /trends  — MANAGER + AGENT allowed; EMPLOYEE forbidden
 * /agents  — MANAGER only
 */
@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@TestPropertySource(properties = "jwt.secret=test-secret-key-for-testing-purposes-only-minimum-32-chars")
class DashboardControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean  DashboardService dashboardService;

    private DashboardStatsResponse emptyStats() {
        return DashboardStatsResponse.builder()
                .totalRequests(0L).openRequests(0L).resolvedRequests(0L)
                .avgResolutionHours(0.0).slaComplianceRate(0.0)
                .requestsByCategory(Map.of()).requestsByPriority(Map.of())
                .requestsByStatus(Map.of()).slaByCategory(Map.of()).build();
    }

    // ── /stats ───────────────────────────────────────────────────────────────

    @Test
    void stats_asManager_returns200() throws Exception {
        when(dashboardService.getStats()).thenReturn(emptyStats());
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + JwtTokenFactory.managerToken()))
                .andExpect(status().isOk());
    }

    @Test
    void stats_asAgent_returns200() throws Exception {
        when(dashboardService.getStats()).thenReturn(emptyStats());
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + JwtTokenFactory.agentToken()))
                .andExpect(status().isOk());
    }

    @Test
    void stats_asEmployee_returns403() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + JwtTokenFactory.employeeToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void stats_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isUnauthorized());
    }

    // ── /sla (MANAGER only) ───────────────────────────────────────────────────

    @Test
    void sla_asManager_returns200() throws Exception {
        when(dashboardService.getSlaStats()).thenReturn(Map.of("IT_SUPPORT", 0.95));
        mockMvc.perform(get("/api/dashboard/sla")
                        .header("Authorization", "Bearer " + JwtTokenFactory.managerToken()))
                .andExpect(status().isOk());
    }

    @Test
    void sla_asAgent_returns403() throws Exception {
        mockMvc.perform(get("/api/dashboard/sla")
                        .header("Authorization", "Bearer " + JwtTokenFactory.agentToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void sla_asEmployee_returns403() throws Exception {
        mockMvc.perform(get("/api/dashboard/sla")
                        .header("Authorization", "Bearer " + JwtTokenFactory.employeeToken()))
                .andExpect(status().isForbidden());
    }

    // ── /trends ───────────────────────────────────────────────────────────────

    @Test
    void trends_asManager_returns200() throws Exception {
        when(dashboardService.getDailyVolumeTrend(7)).thenReturn(Map.of("2026-06-01", 3L));
        mockMvc.perform(get("/api/dashboard/trends")
                        .header("Authorization", "Bearer " + JwtTokenFactory.managerToken()))
                .andExpect(status().isOk());
    }

    @Test
    void trends_asAgent_returns200() throws Exception {
        when(dashboardService.getDailyVolumeTrend(7)).thenReturn(Map.of());
        mockMvc.perform(get("/api/dashboard/trends")
                        .header("Authorization", "Bearer " + JwtTokenFactory.agentToken()))
                .andExpect(status().isOk());
    }

    @Test
    void trends_asEmployee_returns403() throws Exception {
        mockMvc.perform(get("/api/dashboard/trends")
                        .header("Authorization", "Bearer " + JwtTokenFactory.employeeToken()))
                .andExpect(status().isForbidden());
    }

    // ── /agents (MANAGER only) ────────────────────────────────────────────────

    @Test
    void agents_asManager_returns200() throws Exception {
        when(dashboardService.getAgentStats()).thenReturn(List.of());
        mockMvc.perform(get("/api/dashboard/agents")
                        .header("Authorization", "Bearer " + JwtTokenFactory.managerToken()))
                .andExpect(status().isOk());
    }

    @Test
    void agents_asAgent_returns403() throws Exception {
        mockMvc.perform(get("/api/dashboard/agents")
                        .header("Authorization", "Bearer " + JwtTokenFactory.agentToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void agents_asEmployee_returns403() throws Exception {
        mockMvc.perform(get("/api/dashboard/agents")
                        .header("Authorization", "Bearer " + JwtTokenFactory.employeeToken()))
                .andExpect(status().isForbidden());
    }
}
