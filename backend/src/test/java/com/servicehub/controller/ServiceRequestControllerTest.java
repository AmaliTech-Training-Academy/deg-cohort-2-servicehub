package com.servicehub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicehub.config.SecurityConfig;
import com.servicehub.dto.*;
import com.servicehub.service.ServiceRequestService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ServiceRequestController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=test-secret-key-for-testing-purposes-only-minimum-32-chars")
class ServiceRequestControllerTest {

    private static final String JWT_SECRET = "test-secret-key-for-testing-purposes-only-minimum-32-chars";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private ServiceRequestService requestService;

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private String token(String email, String role) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .signWith(key)
                .compact();
    }

    private String employeeToken() { return token("employee@test.com", "EMPLOYEE"); }
    private String agentToken()    { return token("agent@test.com", "AGENT"); }
    private String managerToken()  { return token("manager@test.com", "MANAGER"); }

    private ServiceRequestResponse sampleResponse() {
        return responseWithStatus("OPEN", null);
    }

    private ServiceRequestResponse responseWithStatus(String status, String assignedToName) {
        return ServiceRequestResponse.builder()
                .id(1L)
                .title("Fix printer")
                .category("IT_SUPPORT")
                .priority("HIGH")
                .status(status)
                .departmentName("IT Support")
                .requesterName("Test Employee")
                .assignedToName(assignedToName)
                .isOverdue(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .slaDeadline(LocalDateTime.now().plusHours(4))
                .build();
    }

    // -----------------------------------------------------------------------
    // GET /api/requests
    // -----------------------------------------------------------------------

    @Test
    void getAllRequests_withAuth_returns200AndContent() throws Exception {
        Page<ServiceRequestResponse> page = new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 10), 1);
        when(requestService.getAllRequests(0, 10)).thenReturn(page);

        mockMvc.perform(get("/api/requests")
                        .header("Authorization", "Bearer " + employeeToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Fix printer"))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.content[0].category").value("IT_SUPPORT"));
    }

    @Test
    void getAllRequests_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/requests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllRequests_customPageParams_delegatesToService() throws Exception {
        when(requestService.getAllRequests(2, 5)).thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 5), 0));

        mockMvc.perform(get("/api/requests?page=2&size=5")
                        .header("Authorization", "Bearer " + employeeToken()))
                .andExpect(status().isOk());

        verify(requestService).getAllRequests(2, 5);
    }

    // -----------------------------------------------------------------------
    // GET /api/requests/my-requests
    // -----------------------------------------------------------------------

    @Test
    void getMyRequests_withAuth_returns200() throws Exception {
        Page<ServiceRequestResponse> page = new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 10), 1);
        when(requestService.getMyRequests("employee@test.com", 0, 10)).thenReturn(page);

        mockMvc.perform(get("/api/requests/my-requests")
                        .header("Authorization", "Bearer " + employeeToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].requesterName").value("Test Employee"));
    }

    @Test
    void getMyRequests_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/requests/my-requests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyRequests_passesAuthenticatedEmailToService() throws Exception {
        when(requestService.getMyRequests(eq("employee@test.com"), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/requests/my-requests")
                        .header("Authorization", "Bearer " + employeeToken()))
                .andExpect(status().isOk());

        verify(requestService).getMyRequests(eq("employee@test.com"), anyInt(), anyInt());
    }

    // -----------------------------------------------------------------------
    // GET /api/requests/{id}
    // -----------------------------------------------------------------------

    @Test
    void getById_existingRequest_returns200WithDetails() throws Exception {
        when(requestService.getRequestById(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/requests/1")
                        .header("Authorization", "Bearer " + employeeToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Fix printer"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void getById_nonExistentRequest_returns400WithErrorMessage() throws Exception {
        when(requestService.getRequestById(99L)).thenThrow(new RuntimeException("Request not found"));

        mockMvc.perform(get("/api/requests/99")
                        .header("Authorization", "Bearer " + employeeToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Request not found"));
    }

    @Test
    void getById_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/requests/1"))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // POST /api/requests
    // -----------------------------------------------------------------------

    @Test
    void createRequest_validBody_returns200AndResponse() throws Exception {
        ServiceRequestDto dto = new ServiceRequestDto();
        dto.setTitle("Fix laptop");
        dto.setDescription("Won't start");
        dto.setCategory("IT_SUPPORT");
        dto.setPriority("HIGH");

        when(requestService.createRequest(any(), eq("employee@test.com"))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + employeeToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.departmentName").value("IT Support"));
    }

    @Test
    void createRequest_blankTitle_returns400WithFieldError() throws Exception {
        ServiceRequestDto dto = new ServiceRequestDto();
        dto.setTitle("");
        dto.setCategory("IT_SUPPORT");
        dto.setPriority("HIGH");

        mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + employeeToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").exists());
    }

    @Test
    void createRequest_nullCategory_returns400WithFieldError() throws Exception {
        ServiceRequestDto dto = new ServiceRequestDto();
        dto.setTitle("Fix laptop");
        dto.setCategory(null);
        dto.setPriority("HIGH");

        mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + employeeToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.category").exists());
    }

    @Test
    void createRequest_nullPriority_returns400WithFieldError() throws Exception {
        ServiceRequestDto dto = new ServiceRequestDto();
        dto.setTitle("Fix laptop");
        dto.setCategory("IT_SUPPORT");
        dto.setPriority(null);

        mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + employeeToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.priority").exists());
    }

    @Test
    void createRequest_withoutAuth_returns401() throws Exception {
        ServiceRequestDto dto = new ServiceRequestDto();
        dto.setTitle("Fix laptop");
        dto.setCategory("IT_SUPPORT");
        dto.setPriority("HIGH");

        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createRequest_facilitiesCategory_createsSuccessfully() throws Exception {
        ServiceRequestDto dto = new ServiceRequestDto();
        dto.setTitle("Fix AC");
        dto.setCategory("FACILITIES");
        dto.setPriority("LOW");

        ServiceRequestResponse response = ServiceRequestResponse.builder()
                .id(2L).title("Fix AC").category("FACILITIES").priority("LOW")
                .status("OPEN").departmentName("Facilities").requesterName("Test Employee")
                .isOverdue(false).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .slaDeadline(LocalDateTime.now().plusHours(48)).build();

        when(requestService.createRequest(any(), eq("employee@test.com"))).thenReturn(response);

        mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + employeeToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("FACILITIES"))
                .andExpect(jsonPath("$.departmentName").value("Facilities"));
    }

    @Test
    void createRequest_hrCategory_createsSuccessfully() throws Exception {
        ServiceRequestDto dto = new ServiceRequestDto();
        dto.setTitle("Leave request");
        dto.setCategory("HR_REQUEST");
        dto.setPriority("MEDIUM");

        ServiceRequestResponse response = ServiceRequestResponse.builder()
                .id(3L).title("Leave request").category("HR_REQUEST").priority("MEDIUM")
                .status("OPEN").departmentName("HR").requesterName("Test Employee")
                .isOverdue(false).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .slaDeadline(LocalDateTime.now().plusHours(24)).build();

        when(requestService.createRequest(any(), eq("employee@test.com"))).thenReturn(response);

        mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + employeeToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("HR_REQUEST"))
                .andExpect(jsonPath("$.departmentName").value("HR"));
    }

    // -----------------------------------------------------------------------
    // PUT /api/requests/{id}
    // -----------------------------------------------------------------------

    @Test
    void updateRequest_byOwner_returns200() throws Exception {
        UpdateRequestDto dto = new UpdateRequestDto();
        dto.setTitle("Updated title");

        when(requestService.updateRequest(eq(1L), any(), eq("employee@test.com")))
                .thenReturn(sampleResponse());

        mockMvc.perform(put("/api/requests/1")
                        .header("Authorization", "Bearer " + employeeToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void updateRequest_unauthorizedUser_returns403WithError() throws Exception {
        UpdateRequestDto dto = new UpdateRequestDto();
        dto.setTitle("Sneaky update");

        when(requestService.updateRequest(any(), any(), any()))
                .thenThrow(new RuntimeException("Not authorized to update this request"));

        mockMvc.perform(put("/api/requests/1")
                        .header("Authorization", "Bearer " + employeeToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Not authorized to update this request"));
    }

    @Test
    void updateRequest_requestNotFound_returns400() throws Exception {
        when(requestService.updateRequest(eq(99L), any(), any()))
                .thenThrow(new RuntimeException("Request not found"));

        mockMvc.perform(put("/api/requests/99")
                        .header("Authorization", "Bearer " + employeeToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Request not found"));
    }

    @Test
    void updateRequest_withoutAuth_returns401() throws Exception {
        mockMvc.perform(put("/api/requests/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // PUT /api/requests/{id}/status
    // -----------------------------------------------------------------------

    @Test
    void updateStatus_validTransition_returns200WithNewStatus() throws Exception {
        StatusUpdateRequest update = new StatusUpdateRequest();
        update.setNewStatus("ASSIGNED");

        when(requestService.updateStatus(eq(1L), any(), eq("agent@test.com")))
                .thenReturn(responseWithStatus("ASSIGNED", "Test Agent"));

        mockMvc.perform(put("/api/requests/1/status")
                        .header("Authorization", "Bearer " + agentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assignedToName").value("Test Agent"));
    }

    @Test
    void updateStatus_invalidTransition_returns400WithError() throws Exception {
        StatusUpdateRequest update = new StatusUpdateRequest();
        update.setNewStatus("IN_PROGRESS");

        when(requestService.updateStatus(any(), any(), any()))
                .thenThrow(new RuntimeException("Invalid status transition: OPEN -> IN_PROGRESS"));

        mockMvc.perform(put("/api/requests/1/status")
                        .header("Authorization", "Bearer " + agentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid status transition: OPEN -> IN_PROGRESS"));
    }

    @Test
    void updateStatus_withoutAuth_returns401() throws Exception {
        mockMvc.perform(put("/api/requests/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"ASSIGNED\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateStatus_managerCanAdvanceStatus() throws Exception {
        StatusUpdateRequest update = new StatusUpdateRequest();
        update.setNewStatus("ASSIGNED");

        when(requestService.updateStatus(eq(1L), any(), eq("manager@test.com")))
                .thenReturn(responseWithStatus("ASSIGNED", "Test Manager"));

        mockMvc.perform(put("/api/requests/1/status")
                        .header("Authorization", "Bearer " + managerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedToName").value("Test Manager"));
    }
}
