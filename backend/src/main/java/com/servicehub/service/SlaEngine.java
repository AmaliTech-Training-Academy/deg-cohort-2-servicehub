package com.servicehub.service;

import com.servicehub.model.ServiceRequest;
import com.servicehub.model.enums.Priority;
import com.servicehub.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SlaEngine {

    private final SlaService slaService;
    private final ServiceRequestRepository requestRepository;

    @Scheduled(fixedRate = 60_000)
    public void checkBreaches() {
        for (ServiceRequest req : slaService.getOverdueRequests()) {
            if (!req.isSlaBreached()) {
                req.setSlaBreached(true);
                req.setPriority(escalate(req.getPriority()));
                requestRepository.save(req);
            }
        }
    }

    private Priority escalate(Priority current) {
        return switch (current) {
            case LOW    -> Priority.MEDIUM;
            case MEDIUM -> Priority.HIGH;
            case HIGH, CRITICAL -> current;
        };
    }
}
