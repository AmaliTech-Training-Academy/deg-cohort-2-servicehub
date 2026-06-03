package com.servicehub.service;

import com.servicehub.exception.InvalidStatusTransitionException;
import com.servicehub.exception.NotFoundException;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.User;
import com.servicehub.model.enums.Priority;
import com.servicehub.model.enums.RequestCategory;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.model.enums.Role;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock private ServiceRequestRepository requestRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private WorkflowService workflowService;

    private User agent;
    private ServiceRequest openRequest;

    @BeforeEach
    void setUp() {
        agent = User.builder().id(1L).email("agent@test.com")
                .fullName("Test Agent").role(Role.AGENT).password("pass").build();

        openRequest = ServiceRequest.builder()
                .id(1L).title("Fix printer")
                .category(RequestCategory.IT_SUPPORT).priority(Priority.HIGH)
                .status(RequestStatus.OPEN)
                .requester(User.builder().id(2L).email("emp@test.com")
                        .fullName("Employee").role(Role.EMPLOYEE).password("pass").build())
                .createdAt(LocalDateTime.now().minusHours(1))
                .updatedAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    // -----------------------------------------------------------------------
    // not-found paths
    // -----------------------------------------------------------------------

    @Test
    void updateStatus_unknownRequestId_throwsNotFoundException() {
        when(requestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workflowService.updateStatus(99L, "ASSIGNED", "agent@test.com"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Request not found");
    }

    @Test
    void updateStatus_unknownAgentEmail_throwsNotFoundException() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(openRequest));
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workflowService.updateStatus(1L, "ASSIGNED", "nobody@test.com"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found");
    }

    // -----------------------------------------------------------------------
    // valid transitions
    // -----------------------------------------------------------------------

    @Test
    void updateStatus_openToAssigned_setsAssignedToAndFirstResponseAt() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(openRequest));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServiceRequest result = workflowService.updateStatus(1L, "ASSIGNED", "agent@test.com");

        assertThat(result.getStatus()).isEqualTo(RequestStatus.ASSIGNED);
        assertThat(result.getAssignedTo()).isEqualTo(agent);
        assertThat(result.getFirstResponseAt()).isNotNull();
    }

    @Test
    void updateStatus_assigned_doesNotOverwriteExistingFirstResponseAt() {
        openRequest.setStatus(RequestStatus.OPEN);
        LocalDateTime original = LocalDateTime.now().minusMinutes(30);
        openRequest.setFirstResponseAt(original);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(openRequest));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServiceRequest result = workflowService.updateStatus(1L, "ASSIGNED", "agent@test.com");

        assertThat(result.getFirstResponseAt()).isEqualTo(original);
    }

    @Test
    void updateStatus_inProgressToResolved_setsResolvedAt() {
        openRequest.setStatus(RequestStatus.IN_PROGRESS);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(openRequest));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServiceRequest result = workflowService.updateStatus(1L, "RESOLVED", "agent@test.com");

        assertThat(result.getStatus()).isEqualTo(RequestStatus.RESOLVED);
        assertThat(result.getResolvedAt()).isNotNull();
    }

    @Test
    void updateStatus_persistsViaRepository() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(openRequest));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        workflowService.updateStatus(1L, "ASSIGNED", "agent@test.com");

        verify(requestRepository).save(openRequest);
    }

    // -----------------------------------------------------------------------
    // validateTransition — all invalid paths
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
        "OPEN,IN_PROGRESS",
        "OPEN,RESOLVED",
        "OPEN,CLOSED",
        "ASSIGNED,OPEN",
        "ASSIGNED,RESOLVED",
        "IN_PROGRESS,OPEN",
        "IN_PROGRESS,ASSIGNED",
        "RESOLVED,OPEN",
        "RESOLVED,IN_PROGRESS",
        "CLOSED,OPEN",
        "CLOSED,ASSIGNED",
        "CLOSED,IN_PROGRESS",
        "CLOSED,RESOLVED"
    })
    void validateTransition_illegalMove_throwsInvalidStatusTransitionException(
            String from, String to) {
        RequestStatus current = RequestStatus.valueOf(from);
        RequestStatus next    = RequestStatus.valueOf(to);

        assertThatThrownBy(() -> workflowService.validateTransition(current, next))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @ParameterizedTest
    @CsvSource({
        "OPEN,ASSIGNED",
        "ASSIGNED,IN_PROGRESS",
        "IN_PROGRESS,RESOLVED",
        "RESOLVED,CLOSED"
    })
    void validateTransition_legalMove_doesNotThrow(String from, String to) {
        RequestStatus current = RequestStatus.valueOf(from);
        RequestStatus next    = RequestStatus.valueOf(to);

        workflowService.validateTransition(current, next);
    }
}
