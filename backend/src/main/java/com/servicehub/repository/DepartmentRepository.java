package com.servicehub.repository;

import com.servicehub.model.Department;
import com.servicehub.model.enums.RequestCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByCategory(RequestCategory category);
    Optional<Department> findByNameIgnoreCase(String name);
    List<Department> findByIsActiveTrue();
}
