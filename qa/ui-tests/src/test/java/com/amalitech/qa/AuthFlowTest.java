package com.amalitech.qa;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.testng.annotations.*;

import java.time.Duration;

import static org.testng.Assert.*;

/**
 * UI tests for PR #36 — Angular auth routing and guards.
 * Requires: backend running on :8080 AND ng serve on :4200.
 * Seed users: manager@amalitech.com, agent@amalitech.com, user@amalitech.com (password: password123)
 * Covers: TC-UI-01 through TC-UI-06
 */
public class AuthFlowTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private static final String BASE_URL = "http://localhost:4200";

    @BeforeClass
    public void setup() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @BeforeMethod
    public void clearSession() {
        driver.get(BASE_URL + "/login");
        ((JavascriptExecutor) driver).executeScript("localStorage.clear();");
    }

    // TC-UI-01a: MANAGER login → lands on /dashboard
    @Test(priority = 1)
    public void testManagerLoginRedirectsToDashboard() {
        login("manager@amalitech.com", "password123");
        wait.until(ExpectedConditions.urlContains("/dashboard"));
        assertTrue(driver.getCurrentUrl().endsWith("/dashboard"),
            "MANAGER should land on /dashboard, got: " + driver.getCurrentUrl());
    }

    // TC-UI-01b: AGENT login → lands on /agent-dashboard
    @Test(priority = 2)
    public void testAgentLoginRedirectsToAgentDashboard() {
        login("agent@amalitech.com", "password123");
        wait.until(ExpectedConditions.urlContains("/agent-dashboard"));
        assertTrue(driver.getCurrentUrl().contains("/agent-dashboard"),
            "AGENT should land on /agent-dashboard, got: " + driver.getCurrentUrl());
    }

    // TC-UI-01c: EMPLOYEE login → lands on /my-dashboard
    @Test(priority = 3)
    public void testEmployeeLoginRedirectsToMyDashboard() {
        login("user@amalitech.com", "password123");
        wait.until(ExpectedConditions.urlContains("/my-dashboard"));
        assertTrue(driver.getCurrentUrl().contains("/my-dashboard"),
            "EMPLOYEE should land on /my-dashboard, got: " + driver.getCurrentUrl());
    }

    // TC-UI-02a: Invalid credentials → error message visible
    @Test(priority = 4)
    public void testInvalidCredentialsShowsError() {
        login("manager@amalitech.com", "wrongpassword");
        WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("p.text-red-600")));
        assertTrue(error.getText().contains("Invalid email or password"),
            "Error message should say 'Invalid email or password', got: " + error.getText());
        assertTrue(driver.getCurrentUrl().contains("/login"),
            "Should stay on /login after failed login");
    }

    // TC-UI-02b: Empty form submit → button stays on login (form validation blocks submit)
    @Test(priority = 5)
    public void testEmptyFormDoesNotSubmit() {
        driver.get(BASE_URL + "/login");
        driver.findElement(By.cssSelector("button[type=submit]")).click();
        // Angular reactive form marks fields invalid — page stays at /login
        assertTrue(driver.getCurrentUrl().contains("/login"),
            "Empty form submit should not leave /login");
        // Confirm no API error displayed (client-side block, not server error)
        assertTrue(driver.findElements(By.cssSelector("p.text-red-600")).isEmpty(),
            "No server error should appear when form is invalid");
    }

    // TC-UI-03a: Unauthenticated access to /dashboard → redirect to /login
    @Test(priority = 6)
    public void testUnauthenticatedDashboardRedirects() {
        driver.get(BASE_URL + "/dashboard");
        wait.until(ExpectedConditions.urlContains("/login"));
        assertTrue(driver.getCurrentUrl().contains("/login"),
            "Unauthenticated /dashboard access should redirect to /login");
    }

    // TC-UI-03b: Unauthenticated access to /agent-dashboard → redirect to /login
    @Test(priority = 7)
    public void testUnauthenticatedAgentDashboardRedirects() {
        driver.get(BASE_URL + "/agent-dashboard");
        wait.until(ExpectedConditions.urlContains("/login"));
        assertTrue(driver.getCurrentUrl().contains("/login"),
            "Unauthenticated /agent-dashboard access should redirect to /login");
    }

    // TC-UI-03c: Unauthenticated access to /my-dashboard → redirect to /login
    @Test(priority = 8)
    public void testUnauthenticatedMyDashboardRedirects() {
        driver.get(BASE_URL + "/my-dashboard");
        wait.until(ExpectedConditions.urlContains("/login"));
        assertTrue(driver.getCurrentUrl().contains("/login"),
            "Unauthenticated /my-dashboard access should redirect to /login");
    }

    // TC-UI-04: AGENT tries to navigate to /dashboard (manager route) → redirected to /agent-dashboard
    @Test(priority = 9)
    public void testAgentCannotAccessManagerDashboard() {
        login("agent@amalitech.com", "password123");
        wait.until(ExpectedConditions.urlContains("/agent-dashboard"));
        driver.get(BASE_URL + "/dashboard");
        wait.until(ExpectedConditions.urlContains("/agent-dashboard"));
        assertTrue(driver.getCurrentUrl().contains("/agent-dashboard"),
            "AGENT accessing /dashboard should be redirected to /agent-dashboard");
    }

    // TC-UI-04b: EMPLOYEE tries to navigate to /agent-dashboard → redirected to /my-dashboard
    @Test(priority = 10)
    public void testEmployeeCannotAccessAgentDashboard() {
        login("user@amalitech.com", "password123");
        wait.until(ExpectedConditions.urlContains("/my-dashboard"));
        driver.get(BASE_URL + "/agent-dashboard");
        wait.until(ExpectedConditions.urlContains("/my-dashboard"));
        assertTrue(driver.getCurrentUrl().contains("/my-dashboard"),
            "EMPLOYEE accessing /agent-dashboard should be redirected to /my-dashboard");
    }

    // TC-UI-05a: Logout clears session and redirects to /login
    @Test(priority = 11)
    public void testLogoutRedirectsToLogin() {
        login("manager@amalitech.com", "password123");
        wait.until(ExpectedConditions.urlContains("/dashboard"));

        driver.findElement(By.xpath("//button[contains(text(), 'Sign out') or contains(text(), 'Logout') or contains(text(), 'Log out')]")).click();
        wait.until(ExpectedConditions.urlContains("/login"));
        assertTrue(driver.getCurrentUrl().contains("/login"),
            "After logout, should be on /login");
    }

    // TC-UI-05b: After logout, protected route is blocked again
    @Test(priority = 12)
    public void testProtectedRouteBlockedAfterLogout() {
        login("manager@amalitech.com", "password123");
        wait.until(ExpectedConditions.urlContains("/dashboard"));
        driver.findElement(By.xpath("//button[contains(text(), 'Sign out') or contains(text(), 'Logout') or contains(text(), 'Log out')]")).click();
        wait.until(ExpectedConditions.urlContains("/login"));

        driver.get(BASE_URL + "/dashboard");
        wait.until(ExpectedConditions.urlContains("/login"));
        assertTrue(driver.getCurrentUrl().contains("/login"),
            "After logout, /dashboard should redirect back to /login");
    }

    // TC-UI-06: Expired/invalid token in localStorage → authGuard redirects to /login
    @Test(priority = 13)
    public void testExpiredTokenRedirectsToLogin() {
        // Plant a structurally valid but expired JWT (exp in the past)
        // Header: {"alg":"HS256"} Payload: {"sub":"x","role":"MANAGER","exp":1}
        String expiredToken = "eyJhbGciOiJIUzI1NiJ9"
            + ".eyJzdWIiOiJ4IiwicmVsZSI6Ik1BTkFHRVIiLCJleHAiOjF9"
            + ".fakesignature";
        ((JavascriptExecutor) driver).executeScript(
            "localStorage.setItem('auth_token', arguments[0]);", expiredToken);

        driver.get(BASE_URL + "/dashboard");
        wait.until(ExpectedConditions.urlContains("/login"));
        assertTrue(driver.getCurrentUrl().contains("/login"),
            "Expired token should not pass authGuard — expected /login, got: " + driver.getCurrentUrl());
    }

    @AfterClass
    public void teardown() {
        if (driver != null) driver.quit();
    }

    private void login(String email, String password) {
        driver.get(BASE_URL + "/login");
        wait.until(ExpectedConditions.elementToBeClickable(By.id("email"))).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.cssSelector("button[type=submit]")).click();
    }
}
