package com.servicehub.service;

import com.servicehub.exception.InvalidStatusTransitionException;
import com.servicehub.exception.NotFoundException;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.User;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkflowService {

    private final ServiceRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final SseNotificationService notificationService;

    public ServiceRequest updateStatus(Long id, String newStatus, String agentEmail) {
        ServiceRequest req = requestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Request not found"));
        User agent = userRepository.findByEmail(agentEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));

        RequestStatus next = RequestStatus.valueOf(newStatus);
        validateTransition(req.getStatus(), next);

        LocalDateTime now = LocalDateTime.now();
        req.setStatus(next);
        req.setUpdatedAt(now);
        if (next == RequestStatus.ASSIGNED) {
            req.setAssignedTo(agent);
            if (req.getFirstResponseAt() == null) req.setFirstResponseAt(now);
        }
        if (next == RequestStatus.RESOLVED) {
            req.setResolvedAt(now);
        }
        ServiceRequest saved = requestRepository.save(req);
        try {
            notificationService.notify(saved.getRequester().getId(), SseNotificationService.EVENT_TICKET_UPDATED, saved.getId(), saved.getStatus().name());
            if (saved.getAssignedTo() != null) {
                notificationService.notify(saved.getAssignedTo().getId(), SseNotificationService.EVENT_TICKET_ASSIGNED, saved.getId(), saved.getStatus().name());
            }
        } catch (Exception e) {
            log.warn("SSE notify failed for request {}: {}", saved.getId(), e.getMessage());
        }
        return saved;
    }

    void validateTransition(RequestStatus current, RequestStatus next) {
        boolean valid = switch (current) {
            case OPEN       -> next == RequestStatus.ASSIGNED;
            case ASSIGNED   -> next == RequestStatus.IN_PROGRESS;
            case IN_PROGRESS -> next == RequestStatus.RESOLVED;
            case RESOLVED   -> next == RequestStatus.CLOSED;
            case CLOSED     -> false;
        };
        if (!valid)
            throw new InvalidStatusTransitionException("Invalid status transition: " + current + " -> " + next);
    }
}
