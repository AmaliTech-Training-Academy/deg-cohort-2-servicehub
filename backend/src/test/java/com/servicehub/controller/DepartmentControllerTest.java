package com.servicehub.controller;

import com.servicehub.config.CorsConfig;
import com.servicehub.config.SecurityConfig;
import com.servicehub.model.Department;
import com.servicehub.repository.DepartmentRepository;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UC-12 RBAC matrix for DepartmentController.
 *
 * GET  /api/departments     — public (any authenticated user — SecurityConfig permits /api/departments GET explicitly)
 * GET  /api/departments/{id} — any authenticated user
 * POST /api/departments     — MANAGER only
 * PUT  /api/departments/{id} — MANAGER only
 * DELETE /api/departments/{id} — MANAGER only
 *
 * Finding: GET /api/departments is permitAll() in SecurityConfig (line: .requestMatchers(HttpMethod.GET, "/api/departments").permitAll())
 * so unauthenticated callers can read the department list. This is intentional for the register flow.
 */
@WebMvcTest(DepartmentController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@TestPropertySource(properties = "jwt.secret=test-secret-key-for-testing-purposes-only-minimum-32-chars")
class DepartmentControllerTest {

    private static final String SECRET = "test-secret-key-for-testing-purposes-only-minimum-32-chars";

    @Autowired MockMvc mockMvc;
    @MockBean  DepartmentRepository departmentRepository;

    private String token(String email, String role) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder().subject(email).claim("role", role).signWith(key).compact();
    }

    private String manager()  { return token("mgr@test.com",  "MANAGER");  }
    private String agent()    { return token("agt@test.com",  "AGENT");    }
    private String employee() { return token("emp@test.com",  "EMPLOYEE"); }

    private Department sampleDept() {
        Department d = new Department();
        d.setId(1L); d.setName("IT Support"); d.setIsActive(true);
        return d;
    }

    // ── GET /api/departments — permitAll ──────────────────────────────────────

    @Test
    void getAll_withoutToken_returns200() throws Exception {
        when(departmentRepository.findByIsActiveTrue()).thenReturn(List.of(sampleDept()));
        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("IT Support"));
    }

    @Test
    void getAll_asEmployee_returns200() throws Exception {
        when(departmentRepository.findByIsActiveTrue()).thenReturn(List.of(sampleDept()));
        mockMvc.perform(get("/api/departments")
                        .header("Authorization", "Bearer " + employee()))
                .andExpect(status().isOk());
    }

    // ── GET /api/departments/{id} — any authenticated user ───────────────────

    @Test
    void getById_asAgent_returns200() throws Exception {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(sampleDept()));
        mockMvc.perform(get("/api/departments/1")
                        .header("Authorization", "Bearer " + agent()))
                .andExpect(status().isOk());
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/departments/99")
                        .header("Authorization", "Bearer " + agent()))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/departments — MANAGER only ─────────────────────────────────

    @Test
    void create_asManager_returns200() throws Exception {
        when(departmentRepository.save(any())).thenReturn(sampleDept());
        mockMvc.perform(post("/api/departments")
                        .header("Authorization", "Bearer " + manager())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Legal\",\"category\":\"HR_REQUEST\",\"contactEmail\":\"legal@test.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void create_asAgent_returns403() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .header("Authorization", "Bearer " + agent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Legal\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_asEmployee_returns403() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .header("Authorization", "Bearer " + employee())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Legal\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Legal\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ── DELETE /api/departments/{id} — MANAGER only ──────────────────────────

    @Test
    void delete_asManager_returns204() throws Exception {
        when(departmentRepository.existsById(1L)).thenReturn(true);
        mockMvc.perform(delete("/api/departments/1")
                        .header("Authorization", "Bearer " + manager()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_asAgent_returns403() throws Exception {
        mockMvc.perform(delete("/api/departments/1")
                        .header("Authorization", "Bearer " + agent()))
                .andExpect(status().isForbidden());
    }
}
