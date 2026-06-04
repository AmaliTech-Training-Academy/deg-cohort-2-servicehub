package com.servicehub.service;

import com.servicehub.fixtures.UserFixtures;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.enums.Priority;
import com.servicehub.model.enums.RequestCategory;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.repository.ServiceRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlaEngineTest {

    @Mock SlaService slaService;
    @Mock ServiceRequestRepository requestRepository;
    @InjectMocks SlaEngine engine;

    // ─── Core behaviour ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Unflagged overdue request gets slaBreached=true and priority escalated")
    void checkBreaches_flagsAndEscalates_whenNotYetBreached() {
        ServiceRequest req = buildRequest(Priority.LOW, false);
        when(slaService.getOverdueRequests()).thenReturn(List.of(req));
        when(requestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        engine.checkBreaches();

        assertThat(req.isSlaBreached()).isTrue();
        assertThat(req.getPriority()).isEqualTo(Priority.MEDIUM);
        verify(requestRepository).save(req);
    }

    @Test
    @DisplayName("Already-breached request is skipped — save never called")
    void checkBreaches_skips_whenAlreadyBreached() {
        ServiceRequest req = buildRequest(Priority.LOW, true);
        when(slaService.getOverdueRequests()).thenReturn(List.of(req));

        engine.checkBreaches();

        verify(requestRepository, never()).save(any());
        assertThat(req.getPriority()).isEqualTo(Priority.LOW);
    }

    @Test
    @DisplayName("Empty overdue list causes no saves")
    void checkBreaches_noOp_whenNoOverdueRequests() {
        when(slaService.getOverdueRequests()).thenReturn(List.of());

        engine.checkBreaches();

        verify(requestRepository, never()).save(any());
    }

    // ─── Escalation matrix ──────────────────────────────────────────────────

    @Test @DisplayName("LOW escalates to MEDIUM")
    void escalate_low_toMedium() {
        assertEscalation(Priority.LOW, Priority.MEDIUM);
    }

    @Test @DisplayName("MEDIUM escalates to HIGH")
    void escalate_medium_toHigh() {
        assertEscalation(Priority.MEDIUM, Priority.HIGH);
    }

    @Test @DisplayName("HIGH stays HIGH — ceiling reached")
    void escalate_high_staysHigh() {
        assertEscalation(Priority.HIGH, Priority.HIGH);
    }

    @Test @DisplayName("CRITICAL stays CRITICAL — already at ceiling")
    void escalate_critical_staysCritical() {
        assertEscalation(Priority.CRITICAL, Priority.CRITICAL);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private void assertEscalation(Priority input, Priority expected) {
        ServiceRequest req = buildRequest(input, false);
        when(slaService.getOverdueRequests()).thenReturn(List.of(req));
        when(requestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        engine.checkBreaches();

        assertThat(req.getPriority()).isEqualTo(expected);
    }

    private ServiceRequest buildRequest(Priority priority, boolean alreadyBreached) {
        ServiceRequest req = ServiceRequest.builder()
                .id(1L).title("Overdue request").description("Test")
                .category(RequestCategory.IT_SUPPORT).priority(priority)
                .status(RequestStatus.IN_PROGRESS).requester(UserFixtures.employee())
                .slaDeadline(LocalDateTime.now().minusHours(1))
                .createdAt(LocalDateTime.now().minusHours(5))
                .updatedAt(LocalDateTime.now())
                .build();
        req.setSlaBreached(alreadyBreached);
        return req;
    }
}
