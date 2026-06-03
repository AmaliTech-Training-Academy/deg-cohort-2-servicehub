package com.servicehub.repository;

import com.servicehub.model.StatusTransitionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatusTransitionLogRepository extends JpaRepository<StatusTransitionLog, Long> {
    List<StatusTransitionLog> findByRequestIdOrderByChangedAtAsc(Long requestId);
}
