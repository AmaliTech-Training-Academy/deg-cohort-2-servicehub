package com.servicehub.fixtures;

import com.servicehub.model.ServiceRequest;
import com.servicehub.model.User;
import com.servicehub.model.enums.Priority;
import com.servicehub.model.enums.RequestCategory;
import com.servicehub.model.enums.RequestStatus;

import java.time.LocalDateTime;

/**
 * Canonical test-request builders.
 *
 * Requests are built in a known state so tests only mutate the field
 * they care about. All timestamps are relative to execution time.
 */
public final class ServiceRequestFixtures {

    private ServiceRequestFixtures() {}

    /** OPEN IT_SUPPORT HIGH request — the baseline state for most workflow tests. */
    public static ServiceRequest openRequest(User requester) {
        return ServiceRequest.builder()
                .id(1L)
                .title("Fix printer")
                .description("Printer not working")
                .category(RequestCategory.IT_SUPPORT)
                .priority(Priority.HIGH)
                .status(RequestStatus.OPEN)
                .requester(requester)
                .slaDeadline(LocalDateTime.now().plusHours(4))
                .responseDeadline(LocalDateTime.now().plusHours(1))
                .createdAt(LocalDateTime.now().minusHours(1))
                .updatedAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    /** IN_PROGRESS request with an assigned agent — used for late-stage workflow tests. */
    public static ServiceRequest inProgressRequest(User requester, User assignedAgent) {
        return ServiceRequest.builder()
                .id(1L)
                .title("Fix printer")
                .description("Printer not working")
                .category(RequestCategory.IT_SUPPORT)
                .priority(Priority.HIGH)
                .status(RequestStatus.IN_PROGRESS)
                .requester(requester)
                .assignedTo(assignedAgent)
                .slaDeadline(LocalDateTime.now().plusHours(3))
                .responseDeadline(LocalDateTime.now().plusHours(1))
                .firstResponseAt(LocalDateTime.now().minusMinutes(30))
                .createdAt(LocalDateTime.now().minusHours(1))
                .updatedAt(LocalDateTime.now().minusMinutes(30))
                .build();
    }

    /** Overdue OPEN request — SLA deadline already passed, still active. */
    public static ServiceRequest overdueRequest(User requester) {
        return ServiceRequest.builder()
                .id(1L)
                .title("Overdue request")
                .description("Past its deadline")
                .category(RequestCategory.IT_SUPPORT)
                .priority(Priority.HIGH)
                .status(RequestStatus.OPEN)
                .requester(requester)
                .slaDeadline(LocalDateTime.now().minusHours(2))
                .responseDeadline(LocalDateTime.now().minusHours(3))
                .createdAt(LocalDateTime.now().minusHours(5))
                .updatedAt(LocalDateTime.now().minusHours(5))
                .build();
    }

    /** RESOLVED request — tests that no further transitions are valid. */
    public static ServiceRequest resolvedRequest(User requester, User assignedAgent) {
        return ServiceRequest.builder()
                .id(1L)
                .title("Fix printer")
                .description("Printer not working")
                .category(RequestCategory.IT_SUPPORT)
                .priority(Priority.HIGH)
                .status(RequestStatus.RESOLVED)
                .requester(requester)
                .assignedTo(assignedAgent)
                .slaDeadline(LocalDateTime.now().plusHours(2))
                .responseDeadline(LocalDateTime.now().plusHours(1))
                .firstResponseAt(LocalDateTime.now().minusHours(1))
                .resolvedAt(LocalDateTime.now().minusMinutes(10))
                .createdAt(LocalDateTime.now().minusHours(2))
                .updatedAt(LocalDateTime.now().minusMinutes(10))
                .build();
    }

    /** Open request with a custom ID — when a test needs multiple distinct requests. */
    public static ServiceRequest openRequestWithId(Long id, User requester) {
        return ServiceRequest.builder()
                .id(id)
                .title("Request " + id)
                .description("Test request " + id)
                .category(RequestCategory.IT_SUPPORT)
                .priority(Priority.MEDIUM)
                .status(RequestStatus.OPEN)
                .requester(requester)
                .slaDeadline(LocalDateTime.now().plusHours(24))
                .responseDeadline(LocalDateTime.now().plusHours(4))
                .createdAt(LocalDateTime.now().minusMinutes(30))
                .updatedAt(LocalDateTime.now().minusMinutes(30))
                .build();
    }
}
