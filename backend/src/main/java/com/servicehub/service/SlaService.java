package com.servicehub.service;

import com.servicehub.dto.SlaPolicyUpdateDto;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.SlaPolicy;
import com.servicehub.model.User;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.model.enums.Role;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.SlaPolicyRepository;
import com.servicehub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SlaService {

    private final ServiceRequestRepository requestRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final UserRepository userRepository;

    private static final List<RequestStatus> TERMINAL = List.of(RequestStatus.RESOLVED, RequestStatus.CLOSED);

    public List<ServiceRequest> getResolutionBreaches() {
        return requestRepository.findBySlaDeadlineBeforeAndStatusNotIn(LocalDateTime.now(), TERMINAL);
    }

    public List<ServiceRequest> getResponseBreaches() {
        return requestRepository
                .findByResponseDeadlineBeforeAndFirstResponseAtIsNullAndStatusNotIn(LocalDateTime.now(), TERMINAL);
    }

    public List<ServiceRequest> getAllBreaches() {
        Set<Long> seen = new HashSet<>();
        List<ServiceRequest> all = new ArrayList<>();
        for (ServiceRequest r : getResolutionBreaches()) {
            if (seen.add(r.getId())) all.add(r);
        }
        for (ServiceRequest r : getResponseBreaches()) {
            if (seen.add(r.getId())) all.add(r);
        }
        return all;
    }

    public List<SlaPolicy> getAllPolicies() {
        return slaPolicyRepository.findAll();
    }

    public SlaPolicy updatePolicy(Long id, SlaPolicyUpdateDto dto, String callerEmail) {
        User caller = userRepository.findByEmail(callerEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (caller.getRole() != Role.MANAGER) {
            throw new RuntimeException("Not authorized to update SLA policies");
        }
        SlaPolicy policy = slaPolicyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SLA policy not found"));
        if (dto.getResponseTimeHours() != null) policy.setResponseTimeHours(dto.getResponseTimeHours());
        if (dto.getResolutionTimeHours() != null) policy.setResolutionTimeHours(dto.getResolutionTimeHours());
        return slaPolicyRepository.save(policy);
    }
}
