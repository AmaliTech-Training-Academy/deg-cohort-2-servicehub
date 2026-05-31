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

        // Compute SLA deadline from the matching SLA policy
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime slaDeadline = slaPolicyRepository.findByPriority(priority)
                .map(p -> now.plusHours(p.getResolutionTimeHours()))
                .orElse(now.plusHours(24));

        ServiceRequest req = ServiceRequest.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .category(category)
                .priority(priority)
                .status(RequestStatus.OPEN)
                .requester(requester)
                .department(department)
                .slaDeadline(slaDeadline)
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
            slaPolicyRepository.findByPriority(priority).ifPresent(p ->
                    req.setSlaDeadline(req.getCreatedAt().plusHours(p.getResolutionTimeHours())));
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

        req.setStatus(newStatus);
        req.setAssignedTo(agent);
        req.setUpdatedAt(LocalDateTime.now());
        if (newStatus == RequestStatus.RESOLVED) {
            req.setResolvedAt(LocalDateTime.now());
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
        boolean overdue = req.getSlaDeadline() != null
                && LocalDateTime.now().isAfter(req.getSlaDeadline())
                && req.getStatus() != RequestStatus.RESOLVED
                && req.getStatus() != RequestStatus.CLOSED;
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
                .createdAt(req.getCreatedAt())
                .updatedAt(req.getUpdatedAt())
                .resolvedAt(req.getResolvedAt())
                .isOverdue(overdue)
                .build();
    }
}
