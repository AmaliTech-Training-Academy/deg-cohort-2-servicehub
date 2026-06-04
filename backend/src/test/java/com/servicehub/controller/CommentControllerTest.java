package com.servicehub.controller;

import com.servicehub.config.CorsConfig;
import com.servicehub.config.SecurityConfig;
import com.servicehub.dto.CommentResponse;
import com.servicehub.exception.ForbiddenException;
import com.servicehub.exception.NotFoundException;
import com.servicehub.service.CommentService;
import com.servicehub.support.JwtTokenFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@TestPropertySource(properties = "jwt.secret=test-secret-key-for-testing-purposes-only-minimum-32-chars")
class CommentControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private CommentService commentService;

    private CommentResponse sampleResponse() {
        return CommentResponse.builder()
                .id(1L).authorName("Agent One").body("Looking into this")
                .systemGenerated(false).createdAt(LocalDateTime.now()).build();
    }

    @Test
    void addComment_asAgent_returns201() throws Exception {
        when(commentService.addComment(eq(1L), any(), eq("agt@test.com"))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/requests/1/comments")
                        .header("Authorization", "Bearer " + JwtTokenFactory.agentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Looking into this\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorName").value("Agent One"))
                .andExpect(jsonPath("$.body").value("Looking into this"));
    }

    @Test
    void addComment_asEmployee_returns201() throws Exception {
        CommentResponse resp = CommentResponse.builder()
                .id(2L).authorName("Test Employee").body("Any update?")
                .systemGenerated(false).createdAt(LocalDateTime.now()).build();
        when(commentService.addComment(eq(1L), any(), eq("emp@test.com"))).thenReturn(resp);

        mockMvc.perform(post("/api/requests/1/comments")
                        .header("Authorization", "Bearer " + JwtTokenFactory.employeeToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Any update?\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorName").value("Test Employee"));
    }

    @Test
    void addComment_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/requests/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"hello\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addComment_asManager_returns403() throws Exception {
        mockMvc.perform(post("/api/requests/1/comments")
                        .header("Authorization", "Bearer " + JwtTokenFactory.managerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"hello\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void addComment_blankBody_returns400() throws Exception {
        mockMvc.perform(post("/api/requests/1/comments")
                        .header("Authorization", "Bearer " + JwtTokenFactory.agentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.body").exists());
    }

    @Test
    void addComment_employeeOnOthersRequest_returns403() throws Exception {
        when(commentService.addComment(any(), any(), any()))
                .thenThrow(new ForbiddenException("Employees can only comment on their own requests"));

        mockMvc.perform(post("/api/requests/1/comments")
                        .header("Authorization", "Bearer " + JwtTokenFactory.employeeToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"sneaky\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Employees can only comment on their own requests"));
    }

    @Test
    void addComment_requestNotFound_returns404() throws Exception {
        when(commentService.addComment(eq(99L), any(), any()))
                .thenThrow(new NotFoundException("Request not found"));

        mockMvc.perform(post("/api/requests/99/comments")
                        .header("Authorization", "Bearer " + JwtTokenFactory.agentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"hello\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Request not found"));
    }

    @Test
    void getComments_withAuth_returns200AndList() throws Exception {
        CommentResponse sys = CommentResponse.builder()
                .id(1L).authorName("Agent One").body("[OPEN → ASSIGNED] On it")
                .systemGenerated(true).createdAt(LocalDateTime.now()).build();
        CommentResponse user = CommentResponse.builder()
                .id(2L).authorName("Test Employee").body("Thanks!")
                .systemGenerated(false).createdAt(LocalDateTime.now()).build();

        when(commentService.getComments(1L)).thenReturn(List.of(sys, user));

        mockMvc.perform(get("/api/requests/1/comments")
                        .header("Authorization", "Bearer " + JwtTokenFactory.agentToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].systemGenerated").value(true))
                .andExpect(jsonPath("$[0].body").value("[OPEN → ASSIGNED] On it"))
                .andExpect(jsonPath("$[1].systemGenerated").value(false));
    }

    @Test
    void getComments_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/requests/1/comments"))
                .andExpect(status().isUnauthorized());
    }
}
