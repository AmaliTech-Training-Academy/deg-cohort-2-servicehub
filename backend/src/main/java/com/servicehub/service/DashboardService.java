package com.servicehub.service;

import com.servicehub.dto.DashboardStatsResponse;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ServiceRequestRepository requestRepository;

    public DashboardStatsResponse getStats() {
        long total = requestRepository.count();
        long open = requestRepository.countByStatus(RequestStatus.OPEN);
        long resolved = requestRepository.countByStatus(RequestStatus.RESOLVED)
                      + requestRepository.countByStatus(RequestStatus.CLOSED);
        Double avg = requestRepository.averageResolutionHours();

        Map<String, Long> byCategory = toStringLongMap(requestRepository.countGroupedByCategory());
        Map<String, Long> byPriority = toStringLongMap(requestRepository.countGroupedByPriority());
        Map<String, Long> byStatus   = toStringLongMap(requestRepository.countGroupedByStatus());

        long totalResolved = requestRepository.countByStatus(RequestStatus.RESOLVED)
                           + requestRepository.countByStatus(RequestStatus.CLOSED);
        long onTime = requestRepository.countResolvedWithinSlaByCategory()
                           .stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        double compliance = totalResolved > 0 ? (double) onTime / totalResolved : 0.0;

        return DashboardStatsResponse.builder()
                .totalRequests(total)
                .openRequests(open)
                .resolvedRequests(resolved)
                .avgResolutionHours(avg != null ? avg : 0.0)
                .slaComplianceRate(compliance)
                .requestsByCategory(byCategory)
                .requestsByPriority(byPriority)
                .requestsByStatus(byStatus)
                .slaByCategory(getSlaStats())
                .build();
    }

    public Map<String, Double> getSlaStats() {
        Map<String, Long> totalByCategory = toStringLongMap(requestRepository.countGroupedByCategory());
        Map<String, Long> onTimeByCategory = toStringLongMap(requestRepository.countResolvedWithinSlaByCategory());

        Map<String, Double> result = new LinkedHashMap<>();
        for (String cat : totalByCategory.keySet()) {
            long total = totalByCategory.getOrDefault(cat, 0L);
            long onTime = onTimeByCategory.getOrDefault(cat, 0L);
            result.put(cat, total > 0 ? (double) onTime / total : 0.0);
        }
        return result;
    }

    public Map<String, Long> getDailyVolumeTrend(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : requestRepository.countGroupedByDate(since)) {
            result.put(row[0].toString(), ((Number) row[1]).longValue());
        }
        return result;
    }

    public List<Map<String, Object>> getAgentStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : requestRepository.agentPerformanceStats()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("agentName",          row[0].toString());
            entry.put("totalAssigned",      ((Number) row[1]).longValue());
            entry.put("totalResolved",      ((Number) row[2]).longValue());
            entry.put("avgResolutionHours", row[3] != null ? ((Number) row[3]).doubleValue() : 0.0);
            result.add(entry);
        }
        return result;
    }

    private Map<String, Long> toStringLongMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            map.put(row[0].toString(), ((Number) row[1]).longValue());
        }
        return map;
    }
}
