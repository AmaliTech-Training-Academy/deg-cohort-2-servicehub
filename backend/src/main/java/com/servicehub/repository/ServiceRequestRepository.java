package com.servicehub.repository;

import com.servicehub.model.ServiceRequest;
import com.servicehub.model.enums.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    Page<ServiceRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<ServiceRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId, Pageable pageable);
    List<ServiceRequest> findByStatus(RequestStatus status);
    List<ServiceRequest> findByCategory(RequestCategory category);
    List<ServiceRequest> findByAssignedToId(Long agentId);
    List<ServiceRequest> findByRequesterId(Long requesterId);
    Long countByStatus(RequestStatus status);
    List<ServiceRequest> findBySlaDeadlineBeforeAndStatusNotIn(LocalDateTime deadline, List<RequestStatus> statuses);
    List<ServiceRequest> findByResponseDeadlineBeforeAndFirstResponseAtIsNullAndStatusNotIn(
            LocalDateTime deadline, List<RequestStatus> statuses);

    @Query("SELECT r.category, COUNT(r) FROM ServiceRequest r GROUP BY r.category")
    List<Object[]> countGroupedByCategory();

    @Query("SELECT r.priority, COUNT(r) FROM ServiceRequest r GROUP BY r.priority")
    List<Object[]> countGroupedByPriority();

    @Query("SELECT r.status, COUNT(r) FROM ServiceRequest r GROUP BY r.status")
    List<Object[]> countGroupedByStatus();

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (resolved_at - created_at)) / 3600.0) FROM service_requests WHERE resolved_at IS NOT NULL", nativeQuery = true)
    Double averageResolutionHours();

    @Query(value = "SELECT category, COUNT(*) FROM service_requests WHERE resolved_at IS NOT NULL AND resolved_at <= sla_deadline GROUP BY category", nativeQuery = true)
    List<Object[]> countResolvedWithinSlaByCategory();

    @Query(value = "SELECT CAST(created_at AS date) AS day, COUNT(*) FROM service_requests WHERE created_at >= :since GROUP BY day ORDER BY day", nativeQuery = true)
    List<Object[]> countGroupedByDate(@Param("since") LocalDateTime since);
}
