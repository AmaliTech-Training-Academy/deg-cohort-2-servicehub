package com.servicehub.service;

import com.servicehub.dto.*;
import com.servicehub.model.*;
import com.servicehub.model.enums.*;
import com.servicehub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {
    private final ServiceRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final SlaPolicyRepository slaPolicyRepository;

    public Page<ServiceRequestResponse> getAllRequests(int page, int size) {
        return requestRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(this::toResponse);
    }

    public Page<ServiceRequestResponse> getMyRequests(String email, int page, int size) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return requestRepository.findByRequesterIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size))
                .map(this::toResponse);
    }

    public ServiceRequestResponse getRequestById(Long id) {
        return toResponse(requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found")));
    }

    public ServiceRequestResponse createRequest(ServiceRequestDto dto, String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RequestCategory category = RequestCategory.valueOf(dto.getCategory());
        Priority priority = Priority.valueOf(dto.getPriority());

        // Auto-route to the department matching this category
        Department department = departmentRepository.findByCategory(category).orElse(null);

        // Compute SLA deadlines from the matching policy (category + priority)
        LocalDateTime now = LocalDateTime.now();
        SlaPolicy policy = slaPolicyRepository.findByCategoryAndPriority(category, priority).orElse(null);
        LocalDateTime resolutionDeadline = policy != null ? now.plusHours(policy.getResolutionTimeHours()) : now.plusHours(24);
        LocalDateTime responseDeadline   = policy != null ? now.plusHours(policy.getResponseTimeHours())  : now.plusHours(4);

        ServiceRequest req = ServiceRequest.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .category(category)
                .priority(priority)
                .status(RequestStatus.OPEN)
                .requester(requester)
                .department(department)
                .slaDeadline(resolutionDeadline)
                .responseDeadline(responseDeadline)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toResponse(requestRepository.save(req));
    }

    public ServiceRequestResponse updateRequest(Long id, UpdateRequestDto dto, String email) {
        ServiceRequest req = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!req.getRequester().getId().equals(user.getId()) && user.getRole() != Role.MANAGER) {
            throw new RuntimeException("Not authorized to update this request");
        }
        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            req.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            req.setDescription(dto.getDescription());
        }
        if (dto.getCategory() != null) {
            RequestCategory category = RequestCategory.valueOf(dto.getCategory());
            req.setCategory(category);
            departmentRepository.findByCategory(category).ifPresent(req::setDepartment);
        }
        if (dto.getPriority() != null) {
            Priority priority = Priority.valueOf(dto.getPriority());
            req.setPriority(priority);
        }
        // Recalculate both SLA deadlines whenever category or priority changed
        if (dto.getCategory() != null || dto.getPriority() != null) {
            slaPolicyRepository.findByCategoryAndPriority(req.getCategory(), req.getPriority()).ifPresent(p -> {
                req.setSlaDeadline(req.getCreatedAt().plusHours(p.getResolutionTimeHours()));
                req.setResponseDeadline(req.getCreatedAt().plusHours(p.getResponseTimeHours()));
            });
        }
        req.setUpdatedAt(LocalDateTime.now());
        return toResponse(requestRepository.save(req));
    }

    public ServiceRequestResponse updateStatus(Long id, StatusUpdateRequest update, String agentEmail) {
        ServiceRequest req = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        User agent = userRepository.findByEmail(agentEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RequestStatus newStatus = RequestStatus.valueOf(update.getNewStatus());
        validateStatusTransition(req.getStatus(), newStatus);

        LocalDateTime now = LocalDateTime.now();
        req.setStatus(newStatus);
        req.setAssignedTo(agent);
        req.setUpdatedAt(now);
        if (newStatus == RequestStatus.ASSIGNED && req.getFirstResponseAt() == null) {
            req.setFirstResponseAt(now);
        }
        if (newStatus == RequestStatus.RESOLVED) {
            req.setResolvedAt(now);
        }
        return toResponse(requestRepository.save(req));
    }

    private void validateStatusTransition(RequestStatus current, RequestStatus next) {
        boolean valid = switch (current) {
            case OPEN -> next == RequestStatus.ASSIGNED;
            case ASSIGNED -> next == RequestStatus.IN_PROGRESS;
            case IN_PROGRESS -> next == RequestStatus.RESOLVED;
            case RESOLVED -> next == RequestStatus.CLOSED;
            case CLOSED -> false;
        };
        if (!valid) {
            throw new RuntimeException("Invalid status transition: " + current + " -> " + next);
        }
    }

    private ServiceRequestResponse toResponse(ServiceRequest req) {
        LocalDateTime now = LocalDateTime.now();
        boolean active = req.getStatus() != RequestStatus.RESOLVED && req.getStatus() != RequestStatus.CLOSED;
        boolean overdue         = active && req.getSlaDeadline() != null      && now.isAfter(req.getSlaDeadline());
        boolean responseOverdue = active && req.getResponseDeadline() != null && req.getFirstResponseAt() == null
                                  && now.isAfter(req.getResponseDeadline());
        return ServiceRequestResponse.builder()
                .id(req.getId())
                .title(req.getTitle())
                .description(req.getDescription())
                .category(req.getCategory().name())
                .priority(req.getPriority().name())
                .status(req.getStatus().name())
                .requesterName(req.getRequester().getFullName())
                .assignedToName(req.getAssignedTo() != null ? req.getAssignedTo().getFullName() : null)
                .departmentName(req.getDepartment() != null ? req.getDepartment().getName() : null)
                .slaDeadline(req.getSlaDeadline())
                .responseDeadline(req.getResponseDeadline())
                .firstResponseAt(req.getFirstResponseAt())
                .createdAt(req.getCreatedAt())
                .updatedAt(req.getUpdatedAt())
                .resolvedAt(req.getResolvedAt())
                .isOverdue(overdue)
                .isResponseOverdue(responseOverdue)
                .build();
    }
}
