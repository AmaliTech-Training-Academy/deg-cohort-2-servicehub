package com.servicehub.service;

import com.servicehub.dto.*;
import com.servicehub.exception.ForbiddenException;
import com.servicehub.exception.NotFoundException;
import com.servicehub.model.*;
import com.servicehub.model.enums.*;
import com.servicehub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {
    private final ServiceRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final WorkflowService workflowService;
    private final SlaService slaService;
    private final StatusTransitionLogRepository transitionLogRepository;

    public Page<ServiceRequestResponse> getAllRequests(int page, int size) {
        return requestRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(this::toResponse);
    }

    public Page<ServiceRequestResponse> getMyRequests(String email, int page, int size) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return requestRepository.findByRequesterIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size))
                .map(this::toResponse);
    }

    public ServiceRequestResponse getRequestById(Long id) {
        return toResponse(requestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Request not found")));
    }

    public ServiceRequestResponse createRequest(ServiceRequestDto dto, String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));

        RequestCategory category = RequestCategory.valueOf(dto.getCategory());
        Priority priority = Priority.valueOf(dto.getPriority());

        Department department = departmentRepository.findByCategory(category).orElse(null);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime resolutionDeadline = slaService.computeDeadline(category, priority);
        LocalDateTime responseDeadline   = slaService.computeResponseDeadline(category, priority);

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
                .orElseThrow(() -> new NotFoundException("Request not found"));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!req.getRequester().getId().equals(user.getId()) && user.getRole() != Role.MANAGER) {
            throw new ForbiddenException("Not authorized to update this request");
        }
        if (dto.getTitle() != null && !dto.getTitle().isBlank()) req.setTitle(dto.getTitle());
        if (dto.getDescription() != null) req.setDescription(dto.getDescription());
        if (dto.getCategory() != null) {
            RequestCategory category = RequestCategory.valueOf(dto.getCategory());
            req.setCategory(category);
            departmentRepository.findByCategory(category).ifPresent(req::setDepartment);
        }
        if (dto.getPriority() != null) {
            Priority priority = Priority.valueOf(dto.getPriority());
            req.setPriority(priority);
        }
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
        return toResponse(workflowService.updateStatus(id, update.getNewStatus(), agentEmail, update.getComment()));
    }

    public ServiceRequestResponse toResponse(ServiceRequest req) {
        LocalDateTime now = LocalDateTime.now();
        boolean active = req.getStatus() != RequestStatus.RESOLVED && req.getStatus() != RequestStatus.CLOSED;
        boolean overdue         = active && req.getSlaDeadline() != null      && now.isAfter(req.getSlaDeadline());
        boolean responseOverdue = active && req.getResponseDeadline() != null && req.getFirstResponseAt() == null
                                  && now.isAfter(req.getResponseDeadline());

        String slaStatus;
        if (!active)              slaStatus = "COMPLETED";
        else if (overdue)         slaStatus = "RESOLUTION_BREACHED";
        else if (responseOverdue) slaStatus = "RESPONSE_BREACHED";
        else                      slaStatus = "ON_TRACK";

        Long responseTimeMinutes = req.getFirstResponseAt() != null && req.getCreatedAt() != null
                ? ChronoUnit.MINUTES.between(req.getCreatedAt(), req.getFirstResponseAt()) : null;
        Long resolutionTimeMinutes = req.getResolvedAt() != null && req.getCreatedAt() != null
                ? ChronoUnit.MINUTES.between(req.getCreatedAt(), req.getResolvedAt()) : null;

        List<StatusTransitionLogResponse> history = req.getId() != null
                ? transitionLogRepository.findByRequestIdOrderByChangedAtAsc(req.getId()).stream()
                        .map(t -> StatusTransitionLogResponse.builder()
                                .fromStatus(t.getFromStatus().name())
                                .toStatus(t.getToStatus().name())
                                .changedByName(t.getChangedBy().getFullName())
                                .comment(t.getComment())
                                .changedAt(t.getChangedAt())
                                .build())
                        .toList()
                : List.of();

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
                .slaStatus(slaStatus)
                .responseTimeMinutes(responseTimeMinutes)
                .resolutionTimeMinutes(resolutionTimeMinutes)
                .slaBreached(req.isSlaBreached())
                .transitionHistory(history)
                .build();
    }
}
