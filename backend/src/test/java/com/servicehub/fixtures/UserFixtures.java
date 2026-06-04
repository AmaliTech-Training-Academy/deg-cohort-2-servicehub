package com.servicehub.fixtures;

import com.servicehub.model.User;
import com.servicehub.model.enums.Role;

/**
 * Canonical test-user builders.
 *
 * IDs are stable across all tests so foreign-key relationships
 * (ServiceRequest.requester, Comment.author) stay predictable.
 * Every builder sets a password field to satisfy @NotBlank constraints
 * even though the value is never used in unit/slice tests.
 */
public final class UserFixtures {

    public static final Long   AGENT_ID       = 1L;
    public static final Long   EMPLOYEE_ID    = 2L;
    public static final Long   MANAGER_ID     = 3L;
    public static final Long   OTHER_EMP_ID   = 4L;

    public static final String AGENT_NAME     = "Agent One";
    public static final String EMPLOYEE_NAME  = "Test Employee";
    public static final String MANAGER_NAME   = "Test Manager";
    public static final String OTHER_EMP_NAME = "Other Employee";

    private UserFixtures() {}

    public static User agent() {
        return User.builder()
                .id(AGENT_ID)
                .email("agent@test.com")
                .fullName("Agent One")
                .role(Role.AGENT)
                .password("pass")
                .build();
    }

    public static User employee() {
        return User.builder()
                .id(EMPLOYEE_ID)
                .email("emp@test.com")
                .fullName("Test Employee")
                .role(Role.EMPLOYEE)
                .password("pass")
                .build();
    }

    public static User manager() {
        return User.builder()
                .id(MANAGER_ID)
                .email("mgr@test.com")
                .fullName("Test Manager")
                .role(Role.MANAGER)
                .password("pass")
                .build();
    }

    public static User otherEmployee() {
        return User.builder()
                .id(OTHER_EMP_ID)
                .email("other@test.com")
                .fullName("Other Employee")
                .role(Role.EMPLOYEE)
                .password("pass")
                .build();
    }

    /** Convenience override when a test needs a specific ID. */
    public static User employeeWithId(Long id) {
        return User.builder()
                .id(id)
                .email("emp" + id + "@test.com")
                .fullName("Test Employee " + id)
                .role(Role.EMPLOYEE)
                .password("pass")
                .build();
    }
}
