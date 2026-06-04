package com.amalitech.qa.data;

/**
 * Seed user credentials matching V2 + V4 Flyway migrations.
 * These are the only accounts the Selenium suite is authorised to use.
 *
 * Passwords are intentionally stored here — this is test infrastructure,
 * not production code, and the accounts only exist in the dev database.
 */
public final class TestUsers {

    public record Credentials(String email, String password, String role,
                               String expectedRoute) {}

    public static final Credentials MANAGER = new Credentials(
            "manager@amalitech.com", "password123", "MANAGER", "/dashboard");

    public static final Credentials AGENT = new Credentials(
            "agent@amalitech.com", "password123", "AGENT", "/agent-dashboard");

    public static final Credentials EMPLOYEE = new Credentials(
            "user@amalitech.com", "password123", "EMPLOYEE", "/my-dashboard");

    /** Valid email, wrong password — used for negative login tests. */
    public static final Credentials WRONG_PASSWORD = new Credentials(
            "manager@amalitech.com", "wrongpassword", "NONE", "/login");

    private TestUsers() {}
}
