package com.servicehub.service;

import com.servicehub.model.ServiceRequest;
import com.servicehub.model.enums.Priority;
import com.servicehub.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SlaEngine {

    private final SlaService slaService;
    private final ServiceRequestRepository requestRepository;
    private final SseNotificationService notificationService;

    @Scheduled(fixedRate = 60_000)
    public void checkBreaches() {
        for (ServiceRequest req : slaService.getOverdueRequests()) {
            if (!req.isSlaBreached()) {
                req.setSlaBreached(true);
                req.setPriority(escalate(req.getPriority()));
                ServiceRequest saved = requestRepository.save(req);
                try {
                    notificationService.notify(saved.getRequester().getId(), SseNotificationService.EVENT_SLA_BREACHED, saved.getId(), saved.getPriority().name());
                    if (saved.getAssignedTo() != null) {
                        notificationService.notify(saved.getAssignedTo().getId(), SseNotificationService.EVENT_SLA_BREACHED, saved.getId(), saved.getPriority().name());
                    }
                } catch (Exception e) {
                    log.warn("SSE notify failed for SLA breach on request {}: {}", saved.getId(), e.getMessage());
                }
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
