package com.servicehub.controller;

import com.servicehub.model.Department;
import com.servicehub.repository.DepartmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Tag(name = "Departments", description = "View departments (read-only for all roles; write requires MANAGER)")
public class DepartmentController {

    private final DepartmentRepository departmentRepository;

    @GetMapping
    @Operation(summary = "List all departments")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of departments"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                content = @Content(schema = @Schema(example = "{\"error\": \"Unauthorized: missing or invalid token\"}")))
    })
    public ResponseEntity<List<Department>> getAll() {
        return ResponseEntity.ok(departmentRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Department found"),
        @ApiResponse(responseCode = "404", description = "Department not found"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                content = @Content(schema = @Schema(example = "{\"error\": \"Unauthorized: missing or invalid token\"}")))
    })
    public ResponseEntity<Department> getById(@PathVariable Long id) {
        return departmentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Create a department (MANAGER only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Department created"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                content = @Content(schema = @Schema(example = "{\"error\": \"Unauthorized: missing or invalid token\"}"))),
        @ApiResponse(responseCode = "403", description = "Requires MANAGER role",
                content = @Content(schema = @Schema(example = "{\"error\": \"Forbidden: insufficient permissions for this action\"}")))
    })
    public ResponseEntity<Department> create(@RequestBody Department department) {
        department.setIsActive(true);
        return ResponseEntity.ok(departmentRepository.save(department));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Update a department (MANAGER only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Department updated"),
        @ApiResponse(responseCode = "404", description = "Department not found"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                content = @Content(schema = @Schema(example = "{\"error\": \"Unauthorized: missing or invalid token\"}"))),
        @ApiResponse(responseCode = "403", description = "Requires MANAGER role",
                content = @Content(schema = @Schema(example = "{\"error\": \"Forbidden: insufficient permissions for this action\"}")))
    })
    public ResponseEntity<Department> update(@PathVariable Long id, @RequestBody Department update) {
        return departmentRepository.findById(id).map(dept -> {
            if (update.getName() != null) dept.setName(update.getName());
            if (update.getCategory() != null) dept.setCategory(update.getCategory());
            if (update.getContactEmail() != null) dept.setContactEmail(update.getContactEmail());
            if (update.getIsActive() != null) dept.setIsActive(update.getIsActive());
            return ResponseEntity.ok(departmentRepository.save(dept));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Delete a department (MANAGER only)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Department deleted"),
        @ApiResponse(responseCode = "404", description = "Department not found"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                content = @Content(schema = @Schema(example = "{\"error\": \"Unauthorized: missing or invalid token\"}"))),
        @ApiResponse(responseCode = "403", description = "Requires MANAGER role",
                content = @Content(schema = @Schema(example = "{\"error\": \"Forbidden: insufficient permissions for this action\"}")))
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!departmentRepository.existsById(id)) return ResponseEntity.notFound().build();
        departmentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
