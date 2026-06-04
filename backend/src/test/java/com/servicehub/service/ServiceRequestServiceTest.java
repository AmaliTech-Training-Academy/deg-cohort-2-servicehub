package com.servicehub.service;

import com.servicehub.dto.*;
import com.servicehub.exception.BadRequestException;
import com.servicehub.fixtures.UserFixtures;
import com.servicehub.exception.ForbiddenException;
import com.servicehub.exception.InvalidStatusTransitionException;
import com.servicehub.exception.NotFoundException;
import com.servicehub.model.*;
import com.servicehub.model.enums.*;
import com.servicehub.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceRequestServiceTest {

    @Mock private ServiceRequestRepository requestRepository;
    @Mock private UserRepository userRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private SlaPolicyRepository slaPolicyRepository;
    @Mock private WorkflowService workflowService;
    @Mock private SlaService slaService;

    @InjectMocks private ServiceRequestService service;

    private User employee;
    private User manager;
    private User agent;
    private User otherEmployee;
    private Department itDept;
    private Department facilitiesDept;
    private Department hrDept;
    private SlaPolicy lowPolicy;
    private SlaPolicy mediumPolicy;
    private SlaPolicy highPolicy;
    private SlaPolicy criticalPolicy;
    private ServiceRequest openRequest;

    @BeforeEach
    void setUp() {
        employee      = UserFixtures.employee();
        manager       = UserFixtures.manager();
        agent         = UserFixtures.agent();
        otherEmployee = UserFixtures.otherEmployee();

        itDept = Department.builder().id(1L).name("IT Support")
                .category(RequestCategory.IT_SUPPORT).isActive(true).build();

        facilitiesDept = Department.builder().id(2L).name("Facilities")
                .category(RequestCategory.FACILITIES).isActive(true).build();

        hrDept = Department.builder().id(3L).name("HR")
                .category(RequestCategory.HR_REQUEST).isActive(true).build();

        lowPolicy = SlaPolicy.builder().id(1L).priority(Priority.LOW)
                .responseTimeHours(24).resolutionTimeHours(48).build();

        mediumPolicy = SlaPolicy.builder().id(2L).priority(Priority.MEDIUM)
                .responseTimeHours(8).resolutionTimeHours(24).build();

        highPolicy = SlaPolicy.builder().id(3L).priority(Priority.HIGH)
                .responseTimeHours(2).resolutionTimeHours(4).build();

        criticalPolicy = SlaPolicy.builder().id(4L).priority(Priority.CRITICAL)
                .responseTimeHours(1).resolutionTimeHours(2).build();

        openRequest = ServiceRequest.builder()
                .id(1L).title("Fix printer").description("Printer not working")
                .category(RequestCategory.IT_SUPPORT).priority(Priority.HIGH)
                .status(RequestStatus.OPEN).requester(employee).department(itDept)
                .slaDeadline(LocalDateTime.now().plusHours(4))
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        lenient().when(slaService.computeDeadline(any(), any())).thenReturn(LocalDateTime.now().plusHours(24));
        lenient().when(slaService.computeResponseDeadline(any(), any())).thenReturn(LocalDateTime.now().plusHours(4));
        lenient().when(workflowService.updateStatus(anyLong(), anyString(), anyString(), any())).thenReturn(openRequest);
    }


    @Test
    void createRequest_validDto_returnsResponseWithOpenStatus() {
        ServiceRequestDto dto = new ServiceRequestDto();
        dto.setTitle("Fix laptop");
        dto.setDescription("Won't start");
        dto.setCategory("IT_SUPPORT");
        dto.setPriority("HIGH");

        when(userRepository.findByEmail("emp@test.com")).thenReturn(Optional.of(employee));
        when(departmentRepository.findByCategory(RequestCategory.IT_SUPPORT)).thenReturn(Optional.of(itDept));
        when(requestRepository.save(any())).thenAnswer(inv -> {
            ServiceRequest r = inv.getArgument(0); r.setId(42L); return r;
        });

        ServiceRequestResponse result = service.createRequest(dto, "emp@test.com");

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getTitle()).isEqualTo("Fix laptop");
        assertThat(result.getCategory()).isEqualTo("IT_SUPPORT");
        assertThat(result.getPriority()).isEqualTo("HIGH");
        assertThat(result.getStatus()).isEqualTo("OPEN");
        assertThat(result.getDepartmentName()).isEqualTo("IT Support");
        assertThat(result.getRequesterName()).isEqualTo(UserFixtures.EMPLOYEE_NAME);
    }

    @Test
    void createRequest_itSupportCategory_routesToItDepartment() {
        ServiceRequestDto dto = new ServiceRequestDto();
        dto.setTitle("Network issue");
        dto.setCategory("IT_SUPPORT");
        dto.setPriority("MEDIUM");

        when(userRepository.findByEmail("emp@test.com")).thenReturn(Optional.of(employee));
        when(departmentRepository.findByCategory(RequestCategory.IT_SUPPORT)).thenReturn(Optional.of(itDept));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServiceRequestResponse result = service.createRequest(dto, "emp@test.com");

        assertThat(result.getDepartmentName()).isEqualTo("IT Support");
        assertThat(result.getCategory()).isEqualTo("IT_SUPPORT");
    }

    @Test
    void createRequest_facilitiesCategory_routesToFacilitiesDepartment() {
        ServiceRequestDto dto = new ServiceRequestDto();
        dto.setTitle("Fix AC");
        dto.setCategory("FACILITIES");
        dto.setPriority("LOW");

        when(userRepository.findByEmail("emp@test.com")).thenReturn(Optional.of(employee));
        when(departmentRepository.findByCategory(RequestCategory.FACILITIES)).thenReturn(Optional.of(facilitiesDept));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServiceRequestResponse result = service.createRequest(dto, "emp@test.com");

        assertThat(result.getDepartmentName()).isEqualTo("Facilities");
        assertThat(result.getCategory()).isEqualTo("FACILITIES");
    }

    @Test
    void createRequest_hrCategory_routesToHrDepartment() {
        ServiceRequestDto dto = new ServiceRequestDto();
        dto.setTitle("Leave request");
        dto.setCategory("HR_REQUEST");
        dto.setPriority("MEDIUM");

        when(userRepository.findByEmail("emp@test.com")).thenReturn(Optional.of(employee));
        when(departmentRepository.findByCategory(RequestCategory.HR_REQUEST)).thenReturn(Optional.of(hrDept));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServiceRequestResponse result = service.createRequest(dto, "emp@test.com");

        assertThat(result.getDepartmentName()).isEqualTo("HR");
        assertThat(result.getCategory()).isEqualTo("HR_REQUEST");
    }

    @Test
    void createRequest_criticalPriority_setsShortSlaDeadline() {
        ServiceRequestDto dto = new ServiceRequestDto();
        dto.setTitle("Server down");
        dto.setCategory("IT_SUPPORT");
        dto.setPriority("CRITICAL");

        when(userRepository.findByEmail("emp@test.com")).thenReturn(Optional.of(employee));
        when(departmentRepository.findByCategory(RequestCategory.IT_SUPPORT)).thenReturn(Optional.of(itDept));
        when(slaService.computeDeadline(RequestCategory.IT_SUPPORT, Priority.CRITICAL)).thenReturn(LocalDateTime.now().plusHours(2));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServiceRequestResponse result = service.createRequest(dto, "emp@test.com");

        assertThat(result.getSlaDeadline()).isAfter(LocalDateTime.now().plusHours(1));
        assertThat(result.getSlaDeadline()).isBefore(LocalDateTime.now().plusHours(3));
    }

    @Test
    void createRequest_lowPriority_setsLongSlaDeadline() {
        ServiceRequestDto dto = new ServiceRequestDto();
        dto.setTitle("Low priority task");
        dto.setCategory("IT_SUPPORT");
        dto.setPriority("LOW");

        when(userRepository.findByEmail("emp@test.com")).thenReturn(Optional.of(employee));
        when(departmentRepository.findByCategory(RequestCategory.IT_SUPPORT)).thenReturn(Optional.of(itDept));
        when(slaService.computeDeadline(RequestCategory.IT_SUPPORT, Priority.LOW)).thenReturn(LocalDateTime.now().plusHours(48));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServiceRequestResponse result = service.createRequest(dto, "emp@test.com");

        assertThat(result.getSlaDeadline()).isAfter(LocalDateTime.now().plusHours(47));
    }

    @Test
    void createRequest_noPolicyFound_defaultsTo24HourSla() {
        ServiceRequestDto dto = new ServiceRequestDto();
        dto.setTitle("Test request");
        dto.setCategory("IT_SUPPORT");
        dto.setPriority("LOW");

        when(userRepository.findByEmail("emp@test.com")).thenReturn(Optional.of(employee));
        when(departmentRepository.findByCategory(any())).thenReturn(Optional.empty());
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServiceRequestResponse result = service.createRequest(dto, "emp@test.com");

        assertThat(result.getSlaDeadline()).isAfter(LocalDateTime.now().plusHours(23));
        assertThat(result.getSlaDeadline()).isBefore(LocalDateTime.now().plusHours(25));
    }

    @Test
    void createRequest_unknownUser_throwsRuntimeException() {
        ServiceRequestDto dto = new ServiceRequestDto();
        dto.setTitle("Test");
        dto.setCategory("IT_SUPPORT");
        dto.setPriority("LOW");

        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createRequest(dto, "nobody@test.com"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found");
    }

    // -----------------------------------------------------------------------
    // getAllRequests
    // -----------------------------------------------------------------------

    @Test
    void getAllRequests_returnsPaginatedResults() {
        Page<ServiceRequest> page = new PageImpl<>(List.of(openRequest));
        when(requestRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10))).thenReturn(page);

        Page<ServiceRequestResponse> result = service.getAllRequests(0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Fix printer");
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("OPEN");
    }

    @Test
    void getAllRequests_emptyRepository_returnsEmptyPage() {
        when(requestRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(Page.empty());

        Page<ServiceRequestResponse> result = service.getAllRequests(0, 10);

        assertThat(result.getContent()).isEmpty();
    }


    @Test
    void getMyRequests_returnsOnlyRequestsForThatUser() {
        Page<ServiceRequest> page = new PageImpl<>(List.of(openRequest));
        when(userRepository.findByEmail("emp@test.com")).thenReturn(Optional.of(employee));
        when(requestRepository.findByRequesterIdOrderByCreatedAtDesc(eq(UserFixtures.EMPLOYEE_ID), any())).thenReturn(page);

        Page<ServiceRequestResponse> result = service.getMyRequests("emp@test.com", 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getRequesterName()).isEqualTo(UserFixtures.EMPLOYEE_NAME);
    }

    @Test
    void getMyRequests_unknownUser_throwsRuntimeException() {
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyRequests("nobody@test.com", 0, 10))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found");
    }


    @Test
    void getRequestById_existingId_returnsResponse() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(openRequest));
        when(userRepository.findByEmail("emp@test.com")).thenReturn(Optional.of(employee));

        ServiceRequestResponse result = service.getRequestById(1L, "emp@test.com");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Fix printer");
        assertThat(result.getPriority()).isEqualTo("HIGH");
        assertThat(result.getCategory()).isEqualTo("IT_SUPPORT");
    }

    @Test
    void getRequestById_nonExistentId_throwsRuntimeException() {
        when(requestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRequestById(99L, "emp@test.com"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Request not found");
    }


    @Test
    void updateRequest_ownerCanUpdateTitle() {
        UpdateRequestDto dto = new UpdateRequestDto();
        dto.setTitle("Updated printer fix");

        when(requestRepository.findById(1L)).thenReturn(Optional.of(openRequest));
        when(userRepository.findByEmail("emp@test.com")).thenReturn(Optional.of(employee));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServiceRequestResponse result = service.updateRequest(1L, dto, "emp@test.com");

        assertThat(result.getTitle()).isEqualTo("Updated printer fix");
    }

    @Test
    void updateRequest_managerCanUpdateOthersRequest() {
        UpdateRequestDto dto = new UpdateRequestDto();
        dto.setTitle("Manager-adjusted title");

        when(requestRepository.findById(1L)).thenReturn(Optional.of(openRequest));
        when(userRepository.findByEmail("mgr@test.com")).thenReturn(Optional.of(manager));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServiceRequestResponse result = service.updateRequest(1L, dto, "mgr@test.com");

        assertThat(result.getTitle()).isEqualTo("Manager-adjusted title");
    }

    @Test
    void updateRequest_otherEmployeeCannotUpdate_throwsForbidden() {
        UpdateRequestDto dto = new UpdateRequestDto();
        dto.setTitle("Sneaky edit");

        when(requestRepository.findById(1L)).thenReturn(Optional.of(openRequest));
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(otherEmployee));

        assertThatThrownBy(() -> service.updateRequest(1L, dto, "other@test.com"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Not authorized");
    }

    @Test
    void updateRequest_categoryChange_reroutesToMatchingDepartment() {
        UpdateRequestDto dto = new UpdateRequestDto();
        dto.setCategory("FACILITIES");

        when(requestRepository.findById(1L)).thenReturn(Optional.of(openRequest));
        when(userRepository.findByEmail("emp@test.com")).thenReturn(Optional.of(employee));
        when(departmentRepository.findByCategory(RequestCategory.FACILITIES)).thenReturn(Optional.of(facilitiesDept));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServiceRequestResponse result = service.updateRequest(1L, dto, "emp@test.com");

        assertThat(result.getDepartmentName()).isEqualTo("Facilities");
        assertThat(result.getCategory()).isEqualTo("FACILITIES");
    }

    @Test
    void updateRequest_priorityChange_recalculatesSlaDeadline() {
        UpdateRequestDto dto = new UpdateRequestDto();
        dto.setPriority("CRITICAL");

        when(requestRepository.findById(1L)).thenReturn(Optional.of(openRequest));
        when(userRepository.findByEmail("emp@test.com")).thenReturn(Optional.of(employee));
        when(slaPolicyRepository.findByCategoryAndPriority(RequestCategory.IT_SUPPORT, Priority.CRITICAL)).thenReturn(Optional.of(criticalPolicy));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateRequest(1L, dto, "emp@test.com");

        verify(slaPolicyRepository).findByCategoryAndPriority(RequestCategory.IT_SUPPORT, Priority.CRITICAL);
    }

    @Test
    void updateRequest_nonExistentRequest_throwsRuntimeException() {
        when(requestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateRequest(99L, new UpdateRequestDto(), "emp@test.com"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Request not found");
    }

    @Test
    void updateRequest_blankTitleIgnored_originalTitleKept() {
        UpdateRequestDto dto = new UpdateRequestDto();
        dto.setTitle("   ");

        when(requestRepository.findById(1L)).thenReturn(Optional.of(openRequest));
        when(userRepository.findByEmail("emp@test.com")).thenReturn(Optional.of(employee));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServiceRequestResponse result = service.updateRequest(1L, dto, "emp@test.com");

        assertThat(result.getTitle()).isEqualTo("Fix printer");
    }


    @Test
    void updateStatus_openToAssigned_succeeds() {
        StatusUpdateRequest update = new StatusUpdateRequest();
        update.setNewStatus("ASSIGNED");

        ServiceRequest assigned = requestWithStatus(RequestStatus.ASSIGNED);
        assigned.setAssignedTo(agent);
        when(workflowService.updateStatus(eq(1L), eq("ASSIGNED"), eq("agent@test.com"), any())).thenReturn(assigned);

        ServiceRequestResponse result = service.updateStatus(1L, update, "agent@test.com");

        assertThat(result.getStatus()).isEqualTo("ASSIGNED");
        assertThat(result.getAssignedToName()).isEqualTo(UserFixtures.AGENT_NAME);
    }

    @Test
    void updateStatus_assignedToInProgress_succeeds() {
        StatusUpdateRequest update = new StatusUpdateRequest();
        update.setNewStatus("IN_PROGRESS");

        when(workflowService.updateStatus(eq(2L), eq("IN_PROGRESS"), eq("agent@test.com"), any()))
                .thenReturn(requestWithStatus(RequestStatus.IN_PROGRESS));

        ServiceRequestResponse result = service.updateStatus(2L, update, "agent@test.com");

        assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void updateStatus_inProgressToResolved_setsResolvedAt() {
        StatusUpdateRequest update = new StatusUpdateRequest();
        update.setNewStatus("RESOLVED");

        ServiceRequest resolved = requestWithStatus(RequestStatus.RESOLVED);
        resolved.setResolvedAt(LocalDateTime.now());
        when(workflowService.updateStatus(eq(3L), eq("RESOLVED"), eq("agent@test.com"), any())).thenReturn(resolved);

        ServiceRequestResponse result = service.updateStatus(3L, update, "agent@test.com");

        assertThat(result.getResolvedAt()).isNotNull();
    }

    @Test
    void updateStatus_resolvedToClosed_succeeds() {
        StatusUpdateRequest update = new StatusUpdateRequest();
        update.setNewStatus("CLOSED");

        when(workflowService.updateStatus(eq(4L), eq("CLOSED"), eq("agent@test.com"), any()))
                .thenReturn(requestWithStatus(RequestStatus.CLOSED));

        ServiceRequestResponse result = service.updateStatus(4L, update, "agent@test.com");

        assertThat(result.getStatus()).isEqualTo("CLOSED");
    }


    @Test
    void updateStatus_openToInProgress_throwsInvalidTransition() {
        expectInvalidTransition(openRequest, "IN_PROGRESS");
    }

    @Test
    void updateStatus_openToResolved_throwsInvalidTransition() {
        expectInvalidTransition(openRequest, "RESOLVED");
    }

    @Test
    void updateStatus_openToClosed_throwsInvalidTransition() {
        expectInvalidTransition(openRequest, "CLOSED");
    }

    @Test
    void updateStatus_assignedToOpen_throwsInvalidTransition() {
        expectInvalidTransition(requestWithStatus(RequestStatus.ASSIGNED), "OPEN");
    }

    @Test
    void updateStatus_assignedToResolved_throwsInvalidTransition() {
        expectInvalidTransition(requestWithStatus(RequestStatus.ASSIGNED), "RESOLVED");
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPEN", "ASSIGNED", "IN_PROGRESS", "RESOLVED"})
    void updateStatus_closedToAnyStatus_throwsInvalidTransition(String targetStatus) {
        ServiceRequest closed = requestWithStatus(RequestStatus.CLOSED);
        when(workflowService.updateStatus(eq(closed.getId()), eq(targetStatus), eq("agent@test.com"), any()))
                .thenThrow(new InvalidStatusTransitionException("Invalid status transition: CLOSED -> " + targetStatus));
        StatusUpdateRequest update = new StatusUpdateRequest();
        update.setNewStatus(targetStatus);
        assertThatThrownBy(() -> service.updateStatus(closed.getId(), update, "agent@test.com"))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Invalid status transition");
    }


    @Test
    void getRequestById_openAndPastDeadline_isOverdueTrue() {
        openRequest.setSlaDeadline(LocalDateTime.now().minusMinutes(1));
        openRequest.setStatus(RequestStatus.OPEN);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(openRequest));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));

        assertThat(service.getRequestById(1L, "agent@test.com").getIsOverdue()).isTrue();
    }

    @Test
    void getRequestById_openButDeadlineNotReached_isOverdueFalse() {
        openRequest.setSlaDeadline(LocalDateTime.now().plusHours(2));
        openRequest.setStatus(RequestStatus.OPEN);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(openRequest));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));

        assertThat(service.getRequestById(1L, "agent@test.com").getIsOverdue()).isFalse();
    }

    @Test
    void getRequestById_resolvedAndPastDeadline_isOverdueFalse() {
        openRequest.setSlaDeadline(LocalDateTime.now().minusHours(1));
        openRequest.setStatus(RequestStatus.RESOLVED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(openRequest));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));

        assertThat(service.getRequestById(1L, "agent@test.com").getIsOverdue()).isFalse();
    }

    @Test
    void getRequestById_closedAndPastDeadline_isOverdueFalse() {
        openRequest.setSlaDeadline(LocalDateTime.now().minusHours(1));
        openRequest.setStatus(RequestStatus.CLOSED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(openRequest));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));

        assertThat(service.getRequestById(1L, "agent@test.com").getIsOverdue()).isFalse();
    }


    private ServiceRequest requestWithStatus(RequestStatus status) {
        long id = switch (status) {
            case OPEN -> 1L;
            case ASSIGNED -> 2L;
            case IN_PROGRESS -> 3L;
            case RESOLVED -> 4L;
            case CLOSED -> 5L;
        };
        return ServiceRequest.builder()
                .id(id).title("Test request").category(RequestCategory.IT_SUPPORT)
                .priority(Priority.MEDIUM).status(status).requester(employee)
                .department(itDept).slaDeadline(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now().minusHours(1)).updatedAt(LocalDateTime.now())
                .build();
    }

    private void expectInvalidTransition(ServiceRequest request, String targetStatus) {
        when(workflowService.updateStatus(eq(request.getId()), eq(targetStatus), eq("agent@test.com"), any()))
                .thenThrow(new InvalidStatusTransitionException(
                        "Invalid status transition: " + request.getStatus() + " -> " + targetStatus));
        StatusUpdateRequest update = new StatusUpdateRequest();
        update.setNewStatus(targetStatus);
        assertThatThrownBy(() -> service.updateStatus(request.getId(), update, "agent@test.com"))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Invalid status transition");
    }
}
