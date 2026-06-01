package com.servicehub.service;

import com.servicehub.dto.ServiceRequestDto;
import com.servicehub.dto.ServiceRequestResponse;
import com.servicehub.dto.StatusUpdateRequest;
import com.servicehub.model.*;
import com.servicehub.model.enums.*;
import com.servicehub.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowSlaServiceTest {

    @Mock ServiceRequestRepository requestRepository;
    @Mock UserRepository userRepository;
    @Mock DepartmentRepository departmentRepository;
    @Mock SlaPolicyRepository slaPolicyRepository;

    @InjectMocks ServiceRequestService service;

    private User employee;
    private User agent;
    private User manager;
    private SlaPolicy highPolicy;
    private Department itDept;

    @BeforeEach
    void setUp() {
        employee  = User.builder().id(1L).email("user@test.com").fullName("Test User").role(Role.EMPLOYEE).build();
        agent     = User.builder().id(2L).email("agent@test.com").fullName("Agent One").role(Role.AGENT).build();
        manager   = User.builder().id(3L).email("mgr@test.com").fullName("Manager").role(Role.MANAGER).build();
        highPolicy = SlaPolicy.builder().id(1L).priority(Priority.HIGH)
                .responseTimeHours(1).resolutionTimeHours(4).build();
        itDept    = Department.builder().id(1L).name("IT Support").category(RequestCategory.IT_SUPPORT).build();
    }

    // ─── Status Workflow ────────────────────────────────────────────────────

    @Test @DisplayName("OPEN -> ASSIGNED sets firstResponseAt and assignedTo")
    void openToAssigned_setsFirstResponseAndAgent() {
        ServiceRequest req = buildRequest(RequestStatus.OPEN);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(req));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
        when(requestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        StatusUpdateRequest dto = new StatusUpdateRequest();
        dto.setNewStatus("ASSIGNED");
        ServiceRequestResponse resp = service.updateStatus(1L, dto, "agent@test.com");

        assertThat(resp.getStatus()).isEqualTo("ASSIGNED");
        assertThat(resp.getFirstResponseAt()).isNotNull();
        assertThat(resp.getAssignedToName()).isEqualTo("Agent One");
    }

    @Test @DisplayName("ASSIGNED -> IN_PROGRESS does NOT overwrite assignedTo")
    void assignedToInProgress_doesNotOverwriteAgent() {
        ServiceRequest req = buildRequest(RequestStatus.ASSIGNED);
        req.setAssignedTo(agent);
        req.setFirstResponseAt(LocalDateTime.now().minusHours(1));
        when(requestRepository.findById(1L)).thenReturn(Optional.of(req));
        when(userRepository.findByEmail("mgr@test.com")).thenReturn(Optional.of(manager));
        when(requestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateStatus(1L, new StatusUpdateRequest() {{ setNewStatus("IN_PROGRESS"); }}, "mgr@test.com");

        assertThat(req.getAssignedTo().getFullName()).isEqualTo("Agent One");
    }

    @Test @DisplayName("IN_PROGRESS -> RESOLVED sets resolvedAt and COMPLETED status")
    void inProgressToResolved_setsResolvedAt() {
        ServiceRequest req = buildRequest(RequestStatus.IN_PROGRESS);
        req.setAssignedTo(agent);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(req));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
        when(requestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ServiceRequestResponse resp = service.updateStatus(1L,
                new StatusUpdateRequest() {{ setNewStatus("RESOLVED"); }}, "agent@test.com");

        assertThat(resp.getStatus()).isEqualTo("RESOLVED");
        assertThat(resp.getResolvedAt()).isNotNull();
        assertThat(resp.getSlaStatus()).isEqualTo("COMPLETED");
    }

    @Test @DisplayName("RESOLVED -> CLOSED is valid")
    void resolvedToClosed_isValid() {
        ServiceRequest req = buildRequest(RequestStatus.RESOLVED);
        req.setResolvedAt(LocalDateTime.now().minusMinutes(5));
        when(requestRepository.findById(1L)).thenReturn(Optional.of(req));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
        when(requestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThat(service.updateStatus(1L,
                new StatusUpdateRequest() {{ setNewStatus("CLOSED"); }}, "agent@test.com")
                .getStatus()).isEqualTo("CLOSED");
    }

    @Test @DisplayName("Invalid transition OPEN -> IN_PROGRESS throws")
    void invalidTransition_throws() {
        ServiceRequest req = buildRequest(RequestStatus.OPEN);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(req));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> service.updateStatus(1L,
                new StatusUpdateRequest() {{ setNewStatus("IN_PROGRESS"); }}, "agent@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test @DisplayName("CLOSED is terminal — any further transition throws")
    void closedIsTerminal_throws() {
        ServiceRequest req = buildRequest(RequestStatus.CLOSED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(req));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> service.updateStatus(1L,
                new StatusUpdateRequest() {{ setNewStatus("RESOLVED"); }}, "agent@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid status transition");
    }

    // ─── SLA Engine ─────────────────────────────────────────────────────────

    @Test @DisplayName("createRequest computes both SLA deadlines from policy")
    void createRequest_computesBothSlaDeadlines() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(employee));
        when(departmentRepository.findByCategory(RequestCategory.IT_SUPPORT)).thenReturn(Optional.of(itDept));
        when(slaPolicyRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(highPolicy));
        when(requestRepository.save(any())).thenAnswer(i -> { ServiceRequest r = i.getArgument(0); r.setId(10L); return r; });

        ServiceRequestDto dto = new ServiceRequestDto();
        dto.setTitle("Laptop broken"); dto.setDescription("Cannot boot");
        dto.setCategory("IT_SUPPORT"); dto.setPriority("HIGH");

        ServiceRequestResponse resp = service.createRequest(dto, "user@test.com");

        assertThat(resp.getSlaDeadline()).isAfter(LocalDateTime.now().plusHours(3));
        assertThat(resp.getResponseDeadline()).isAfter(LocalDateTime.now().plusMinutes(50));
        assertThat(resp.getSlaStatus()).isEqualTo("ON_TRACK");
        assertThat(resp.getDepartmentName()).isEqualTo("IT Support");
    }

    // ─── Response Time Tracking ─────────────────────────────────────────────

    @Test @DisplayName("responseTimeMinutes is null before first response")
    void responseTimeMinutes_nullBeforeAssigned() {
        ServiceRequestResponse resp = service.toResponse(buildRequest(RequestStatus.OPEN));
        assertThat(resp.getFirstResponseAt()).isNull();
        assertThat(resp.getResponseTimeMinutes()).isNull();
        assertThat(resp.getSlaStatus()).isEqualTo("ON_TRACK");
    }

    @Test @DisplayName("responseTimeMinutes computed correctly after ASSIGNED")
    void responseTimeMinutes_computedAfterAssigned() {
        ServiceRequest req = buildRequest(RequestStatus.ASSIGNED);
        req.setFirstResponseAt(req.getCreatedAt().plusMinutes(45));
        assertThat(service.toResponse(req).getResponseTimeMinutes()).isEqualTo(45L);
    }

    @Test @DisplayName("isResponseOverdue true when deadline passed and no response yet")
    void isResponseOverdue_trueWhenDeadlinePassed() {
        ServiceRequest req = buildRequest(RequestStatus.OPEN);
        req.setResponseDeadline(LocalDateTime.now().minusHours(1));
        ServiceRequestResponse resp = service.toResponse(req);
        assertThat(resp.getIsResponseOverdue()).isTrue();
        assertThat(resp.getSlaStatus()).isEqualTo("RESPONSE_BREACHED");
    }

    @Test @DisplayName("isResponseOverdue false after response recorded")
    void isResponseOverdue_falseAfterResponseRecorded() {
        ServiceRequest req = buildRequest(RequestStatus.ASSIGNED);
        req.setResponseDeadline(LocalDateTime.now().minusHours(1));
        req.setFirstResponseAt(LocalDateTime.now().minusHours(2));
        assertThat(service.toResponse(req).getIsResponseOverdue()).isFalse();
    }

    // ─── Resolution Time Tracking ───────────────────────────────────────────

    @Test @DisplayName("resolutionTimeMinutes null before RESOLVED")
    void resolutionTimeMinutes_nullBeforeResolved() {
        ServiceRequestResponse resp = service.toResponse(buildRequest(RequestStatus.IN_PROGRESS));
        assertThat(resp.getResolvedAt()).isNull();
        assertThat(resp.getResolutionTimeMinutes()).isNull();
    }

    @Test @DisplayName("resolutionTimeMinutes computed correctly after RESOLVED")
    void resolutionTimeMinutes_computedAfterResolved() {
        ServiceRequest req = buildRequest(RequestStatus.RESOLVED);
        req.setResolvedAt(req.getCreatedAt().plusMinutes(120));
        ServiceRequestResponse resp = service.toResponse(req);
        assertThat(resp.getResolutionTimeMinutes()).isEqualTo(120L);
        assertThat(resp.getSlaStatus()).isEqualTo("COMPLETED");
    }

    // ─── SLA Breach Detection ───────────────────────────────────────────────

    @Test @DisplayName("slaStatus ON_TRACK when within all deadlines")
    void slaStatus_onTrack() {
        ServiceRequest req = buildRequest(RequestStatus.IN_PROGRESS);
        req.setSlaDeadline(LocalDateTime.now().plusHours(2));
        req.setResponseDeadline(LocalDateTime.now().plusMinutes(30));
        req.setFirstResponseAt(LocalDateTime.now().minusMinutes(10));
        ServiceRequestResponse resp = service.toResponse(req);
        assertThat(resp.getSlaStatus()).isEqualTo("ON_TRACK");
        assertThat(resp.getIsOverdue()).isFalse();
        assertThat(resp.getIsResponseOverdue()).isFalse();
    }

    @Test @DisplayName("slaStatus RESOLUTION_BREACHED when past resolution deadline")
    void slaStatus_resolutionBreached() {
        ServiceRequest req = buildRequest(RequestStatus.IN_PROGRESS);
        req.setSlaDeadline(LocalDateTime.now().minusHours(1));
        req.setResponseDeadline(LocalDateTime.now().minusHours(5));
        req.setFirstResponseAt(LocalDateTime.now().minusHours(4));
        ServiceRequestResponse resp = service.toResponse(req);
        assertThat(resp.getIsOverdue()).isTrue();
        assertThat(resp.getSlaStatus()).isEqualTo("RESOLUTION_BREACHED");
    }

    @Test @DisplayName("slaStatus COMPLETED overrides any breach for RESOLVED/CLOSED")
    void slaStatus_completedOverridesBreached() {
        ServiceRequest req = buildRequest(RequestStatus.RESOLVED);
        req.setSlaDeadline(LocalDateTime.now().minusHours(1));
        req.setResolvedAt(LocalDateTime.now().minusMinutes(5));
        ServiceRequestResponse resp = service.toResponse(req);
        assertThat(resp.getSlaStatus()).isEqualTo("COMPLETED");
        assertThat(resp.getIsOverdue()).isFalse();
    }

    @Test @DisplayName("SlaService.getResolutionBreaches delegates to correct repository method")
    void slaService_getResolutionBreaches() {
        SlaService slaService = new SlaService(requestRepository, slaPolicyRepository, userRepository);
        when(requestRepository.findBySlaDeadlineBeforeAndStatusNotIn(any(), anyList()))
                .thenReturn(List.of(buildRequest(RequestStatus.IN_PROGRESS)));
        assertThat(slaService.getResolutionBreaches()).hasSize(1);
    }

    @Test @DisplayName("SlaService.getResponseBreaches delegates to correct repository method")
    void slaService_getResponseBreaches() {
        SlaService slaService = new SlaService(requestRepository, slaPolicyRepository, userRepository);
        when(requestRepository.findByResponseDeadlineBeforeAndFirstResponseAtIsNullAndStatusNotIn(any(), anyList()))
                .thenReturn(List.of(buildRequest(RequestStatus.OPEN)));
        assertThat(slaService.getResponseBreaches()).hasSize(1);
    }

    // ─── Helper ─────────────────────────────────────────────────────────────

    private ServiceRequest buildRequest(RequestStatus status) {
        return ServiceRequest.builder()
                .id(1L).title("Test request").description("Test")
                .category(RequestCategory.IT_SUPPORT).priority(Priority.HIGH)
                .status(status).requester(employee).department(itDept)
                .slaDeadline(LocalDateTime.now().plusHours(4))
                .responseDeadline(LocalDateTime.now().plusHours(1))
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
