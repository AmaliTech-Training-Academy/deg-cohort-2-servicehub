package com.servicehub.controller;

import com.servicehub.config.CorsConfig;
import com.servicehub.config.SecurityConfig;
import com.servicehub.model.SlaPolicy;
import com.servicehub.service.ServiceRequestService;
import com.servicehub.service.SlaService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RBAC matrix for SlaController.
 * Functional coverage lives in the external QA suite (PR #89).
 * This suite proves @PreAuthorize enforcement without a live backend.
 *
 * All SLA breach + policy endpoints are MANAGER-only.
 *
 * Finding (noted, not a bug): GET /api/sla/policies is MANAGER-only — AGENTs cannot
 * view SLA policy configuration. Consistent with the RBAC spec but worth confirming
 * with the team (agents may need visibility to understand breach timelines).
 */
@WebMvcTest(SlaController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@TestPropertySource(properties = "jwt.secret=test-secret-key-for-testing-purposes-only-minimum-32-chars")
class SlaControllerTest {

    private static final String SECRET = "test-secret-key-for-testing-purposes-only-minimum-32-chars";

    @Autowired MockMvc mockMvc;
    @MockBean  SlaService slaService;
    @MockBean  ServiceRequestService serviceRequestService;

    private String token(String email, String role) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder().subject(email).claim("role", role).signWith(key).compact();
    }

    private String manager()  { return token("mgr@test.com", "MANAGER");  }
    private String agent()    { return token("agt@test.com", "AGENT");    }
    private String employee() { return token("emp@test.com", "EMPLOYEE"); }

    // ── GET /api/sla/breaches ─────────────────────────────────────────────────

    @Test
    void breaches_asManager_returns200() throws Exception {
        when(slaService.getAllBreaches()).thenReturn(List.of());
        mockMvc.perform(get("/api/sla/breaches")
                        .header("Authorization", "Bearer " + manager()))
                .andExpect(status().isOk());
    }

    @Test
    void breaches_asAgent_returns403() throws Exception {
        mockMvc.perform(get("/api/sla/breaches")
                        .header("Authorization", "Bearer " + agent()))
                .andExpect(status().isForbidden());
    }

    @Test
    void breaches_asEmployee_returns403() throws Exception {
        mockMvc.perform(get("/api/sla/breaches")
                        .header("Authorization", "Bearer " + employee()))
                .andExpect(status().isForbidden());
    }

    @Test
    void breaches_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/sla/breaches"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/sla/policies ─────────────────────────────────────────────────

    @Test
    void policies_asManager_returns200() throws Exception {
        SlaPolicy policy = new SlaPolicy();
        policy.setId(1L); policy.setResponseTimeHours(2); policy.setResolutionTimeHours(8);
        when(slaService.getAllPolicies()).thenReturn(List.of(policy));

        mockMvc.perform(get("/api/sla/policies")
                        .header("Authorization", "Bearer " + manager()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].responseTimeHours").value(2));
    }

    @Test
    void policies_asAgent_returns403() throws Exception {
        mockMvc.perform(get("/api/sla/policies")
                        .header("Authorization", "Bearer " + agent()))
                .andExpect(status().isForbidden());
    }

    @Test
    void policies_asEmployee_returns403() throws Exception {
        mockMvc.perform(get("/api/sla/policies")
                        .header("Authorization", "Bearer " + employee()))
                .andExpect(status().isForbidden());
    }

    // ── PUT /api/sla/policies/{id} ────────────────────────────────────────────

    @Test
    void updatePolicy_asManager_returns200() throws Exception {
        SlaPolicy updated = new SlaPolicy();
        updated.setId(1L); updated.setResponseTimeHours(3); updated.setResolutionTimeHours(12);
        when(slaService.updatePolicy(eq(1L), any(), any())).thenReturn(updated);

        mockMvc.perform(put("/api/sla/policies/1")
                        .header("Authorization", "Bearer " + manager())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responseTimeHours\":3,\"resolutionTimeHours\":12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseTimeHours").value(3));
    }

    @Test
    void updatePolicy_asAgent_returns403() throws Exception {
        mockMvc.perform(put("/api/sla/policies/1")
                        .header("Authorization", "Bearer " + agent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responseTimeHours\":3,\"resolutionTimeHours\":12}"))
                .andExpect(status().isForbidden());
    }
}
