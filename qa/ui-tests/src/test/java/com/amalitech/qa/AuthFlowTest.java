package com.amalitech.qa;

import com.amalitech.qa.base.BaseTest;
import com.amalitech.qa.data.TestUsers;
import com.amalitech.qa.pages.*;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * UI tests — Angular auth routing and role-based guards.
 *
 * Covers TC-UI-01 through TC-UI-06.
 * Requires: backend on :8080, Angular on :4200, seed data V1–V6.
 *
 * Test data:  TestUsers — no credentials in test methods.
 * DOM access: via Page Objects only — no raw selectors or driver calls here.
 * Waits:      BasePage.waitForUrl() / waitVisible() — no Thread.sleep().
 */
public class AuthFlowTest extends BaseTest {

    // ── TC-UI-01: Role-based redirect after login ─────────────────────────────

    @Test(priority = 1, description = "TC-UI-01a: MANAGER lands on /dashboard after login")
    public void managerLoginRedirectsToDashboard() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.login(TestUsers.MANAGER);

        ManagerDashboardPage dashboard = new ManagerDashboardPage(driver);
        assertTrue(dashboard.isLoaded(),
                "MANAGER should land on /dashboard — got: " + driver.getCurrentUrl());
    }

    @Test(priority = 2, description = "TC-UI-01b: AGENT lands on /agent-dashboard after login")
    public void agentLoginRedirectsToAgentDashboard() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.login(TestUsers.AGENT);

        AgentDashboardPage dashboard = new AgentDashboardPage(driver);
        assertTrue(dashboard.isLoaded(),
                "AGENT should land on /agent-dashboard — got: " + driver.getCurrentUrl());
    }

    @Test(priority = 3, description = "TC-UI-01c: EMPLOYEE lands on /my-dashboard after login")
    public void employeeLoginRedirectsToMyDashboard() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.login(TestUsers.EMPLOYEE);

        EmployeeDashboardPage dashboard = new EmployeeDashboardPage(driver);
        assertTrue(dashboard.isLoaded(),
                "EMPLOYEE should land on /my-dashboard — got: " + driver.getCurrentUrl());
    }

    // ── TC-UI-02: Login failure UX ────────────────────────────────────────────

    @Test(priority = 4, description = "TC-UI-02a: Wrong password shows inline error, stays on /login")
    public void invalidCredentialsShowsError() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.login(TestUsers.WRONG_PASSWORD);

        assertTrue(loginPage.isErrorVisible(),
                "Error message should appear after invalid credentials");
        assertTrue(loginPage.getErrorText().contains("Invalid email or password"),
                "Error text mismatch: " + loginPage.getErrorText());
        assertTrue(loginPage.isOnLoginPage(),
                "Should remain on /login after failed login");
    }

    @Test(priority = 5, description = "TC-UI-02b: Empty form submit is blocked by client-side validation")
    public void emptyFormDoesNotSubmit() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.open();
        loginPage.clickSubmit();

        assertTrue(loginPage.isOnLoginPage(),
                "Empty form submit should not leave /login");
        assertTrue(loginPage.isErrorAbsent(),
                "No server-side error should appear for client-invalid form");
    }

    // ── TC-UI-03: Unauthenticated access → guard redirects ───────────────────

    @Test(priority = 6, description = "TC-UI-03a: Unauthenticated /dashboard access → /login")
    public void unauthenticatedDashboardRedirects() {
        ManagerDashboardPage dashboard = new ManagerDashboardPage(driver);
        dashboard.navigateTo();
        dashboard.waitForUrl("/login");

        assertTrue(dashboard.isOnPage("/login"),
                "AuthGuard should redirect unauthenticated /dashboard access to /login");
    }

    @Test(priority = 7, description = "TC-UI-03b: Unauthenticated /agent-dashboard → /login")
    public void unauthenticatedAgentDashboardRedirects() {
        AgentDashboardPage dashboard = new AgentDashboardPage(driver);
        dashboard.navigateTo();
        dashboard.waitForUrl("/login");

        assertTrue(dashboard.isOnPage("/login"),
                "AuthGuard should redirect unauthenticated /agent-dashboard to /login");
    }

    @Test(priority = 8, description = "TC-UI-03c: Unauthenticated /my-dashboard → /login")
    public void unauthenticatedMyDashboardRedirects() {
        EmployeeDashboardPage dashboard = new EmployeeDashboardPage(driver);
        dashboard.navigateTo();
        dashboard.waitForUrl("/login");

        assertTrue(dashboard.isOnPage("/login"),
                "AuthGuard should redirect unauthenticated /my-dashboard to /login");
    }

    // ── TC-UI-04: Cross-role route guards ─────────────────────────────────────

    @Test(priority = 9, description = "TC-UI-04a: AGENT accessing /dashboard is redirected to /agent-dashboard")
    public void agentCannotAccessManagerDashboard() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.login(TestUsers.AGENT);
        new AgentDashboardPage(driver).isLoaded();

        new ManagerDashboardPage(driver).navigateTo();

        AgentDashboardPage agentDash = new AgentDashboardPage(driver);
        assertTrue(agentDash.isLoaded(),
                "AGENT accessing /dashboard should be redirected to /agent-dashboard");
    }

    @Test(priority = 10, description = "TC-UI-04b: EMPLOYEE accessing /agent-dashboard is redirected to /my-dashboard")
    public void employeeCannotAccessAgentDashboard() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.login(TestUsers.EMPLOYEE);
        new EmployeeDashboardPage(driver).isLoaded();

        new AgentDashboardPage(driver).navigateTo();

        EmployeeDashboardPage empDash = new EmployeeDashboardPage(driver);
        assertTrue(empDash.isLoaded(),
                "EMPLOYEE accessing /agent-dashboard should be redirected to /my-dashboard");
    }

    // ── TC-UI-05: Logout ──────────────────────────────────────────────────────

    @Test(priority = 11, description = "TC-UI-05a: Logout clears session and navigates to /login")
    public void logoutRedirectsToLogin() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.login(TestUsers.MANAGER);

        ManagerDashboardPage dashboard = new ManagerDashboardPage(driver);
        dashboard.isLoaded();
        dashboard.logout();
        dashboard.waitForUrl("/login");

        assertTrue(dashboard.isOnPage("/login"),
                "After logout, should be on /login");
    }

    @Test(priority = 12, description = "TC-UI-05b: Protected route is blocked after logout")
    public void protectedRouteBlockedAfterLogout() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.login(TestUsers.MANAGER);

        ManagerDashboardPage dashboard = new ManagerDashboardPage(driver);
        dashboard.isLoaded();
        dashboard.logout();
        dashboard.waitForUrl("/login");

        dashboard.navigateTo();
        dashboard.waitForUrl("/login");

        assertTrue(dashboard.isOnPage("/login"),
                "After logout, /dashboard should redirect back to /login");
    }

    // ── TC-UI-06: Token integrity ─────────────────────────────────────────────

    @Test(priority = 13, description = "TC-UI-06: Expired/tampered token in localStorage → AuthGuard redirects to /login")
    public void expiredTokenInLocalStorageRedirects() {
        // Structurally valid JWT with exp=1 (long past) — Angular's guard
        // must detect this and redirect, not grant access.
        String expiredToken = "eyJhbGciOiJIUzI1NiJ9"
                + ".eyJzdWIiOiJ4Iiwicm9sZSI6Ik1BTkFHRVIiLCJleHAiOjF9"
                + ".fakesignature";

        LoginPage loginPage = new LoginPage(driver);
        loginPage.plantToken("auth_token", expiredToken);

        new ManagerDashboardPage(driver).navigateTo();
        new ManagerDashboardPage(driver).waitForUrl("/login");

        assertTrue(loginPage.isOnLoginPage(),
                "Expired token must not pass AuthGuard — expected /login, got: " + driver.getCurrentUrl());
    }
}
