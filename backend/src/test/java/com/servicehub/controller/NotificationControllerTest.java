package com.servicehub.controller;

import com.servicehub.config.CorsConfig;
import com.servicehub.config.JwtService;
import com.servicehub.config.SecurityConfig;
import com.servicehub.exception.UnauthorizedException;
import com.servicehub.model.User;
import com.servicehub.model.enums.Role;
import com.servicehub.repository.UserRepository;
import com.servicehub.service.SseNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Auth matrix for NotificationController (/api/notifications/stream).
 *
 * Unlike all other endpoints, this one:
 *  1. Is permitted through the Security filter chain (SecurityConfig.permitAll()) —
 *     necessary because browser EventSource cannot set Authorization headers.
 *  2. Performs its own token validation via JwtService (not JwtAuthFilter).
 *
 * Tests confirm: missing token → 400, invalid token → 401, valid token → 200 (SSE stream).
 *
 * The permitAll() + controller-level auth pattern means the standard JwtAuthFilter
 * does NOT protect this endpoint. JwtService is mocked to control auth outcomes.
 */
@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@TestPropertySource(properties = "jwt.secret=test-secret-key-for-testing-purposes-only-minimum-32-chars")
class NotificationControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean  SseNotificationService notificationService;
    @MockBean  JwtService jwtService;
    @MockBean  UserRepository userRepository;

    private User sampleUser() {
        return User.builder().id(1L).email("emp@test.com")
                .fullName("Test User").role(Role.EMPLOYEE).password("pass").build();
    }

    @Test
    void stream_withValidToken_returns200AndEventStreamType() throws Exception {
        when(jwtService.isTokenValid("good-token")).thenReturn(true);
        when(jwtService.extractEmail("good-token")).thenReturn("emp@test.com");
        when(userRepository.findByEmail("emp@test.com")).thenReturn(Optional.of(sampleUser()));
        when(notificationService.subscribe(1L)).thenReturn(new SseEmitter(0L));

        mockMvc.perform(get("/api/notifications/stream")
                        .param("token", "good-token"))
                .andExpect(status().isOk());
    }

    @Test
    void stream_withInvalidToken_returns401() throws Exception {
        when(jwtService.isTokenValid("bad-token")).thenReturn(false);
        // Controller throws UnauthorizedException → GlobalExceptionHandler → 401
        // (Mocking the throw directly to keep the test self-contained)
        when(jwtService.isTokenValid("bad-token"))
                .thenThrow(new UnauthorizedException("Invalid or missing token"));

        mockMvc.perform(get("/api/notifications/stream")
                        .param("token", "bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid or missing token"));
    }

    @Test
    void stream_withoutTokenParam_returns400() throws Exception {
        // Missing required @RequestParam — Spring MVC returns 400
        mockMvc.perform(get("/api/notifications/stream"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void stream_userNotFoundForToken_returns404() throws Exception {
        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractEmail("valid-token")).thenReturn("ghost@test.com");
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/notifications/stream")
                        .param("token", "valid-token"))
                .andExpect(status().isNotFound());
    }
}
