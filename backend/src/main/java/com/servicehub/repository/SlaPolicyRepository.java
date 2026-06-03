package com.servicehub.repository;

import com.servicehub.model.SlaPolicy;
import com.servicehub.model.enums.Priority;
import com.servicehub.model.enums.RequestCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {
    Optional<SlaPolicy> findByCategoryAndPriority(RequestCategory category, Priority priority);
}
