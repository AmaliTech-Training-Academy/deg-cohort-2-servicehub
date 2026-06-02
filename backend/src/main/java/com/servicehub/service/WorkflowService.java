package com.servicehub.service;

import com.servicehub.exception.InvalidStatusTransitionException;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.User;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final ServiceRequestRepository requestRepository;
    private final UserRepository userRepository;

    public ServiceRequest updateStatus(Long id, String newStatus, String agentEmail) {
        ServiceRequest req = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        User agent = userRepository.findByEmail(agentEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

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
        return requestRepository.save(req);
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
