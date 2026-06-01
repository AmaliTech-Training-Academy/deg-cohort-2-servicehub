package com.amalitech.qa;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.testng.annotations.*;
import java.time.Duration;
import static org.testng.Assert.*;

// Page object stub — full implementation tracked in Issue #24
// These tests target the Angular SPA at localhost:4200
// Requires: docker-compose up --build AND ng serve running
public class DashboardPageTest {

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

    @Test(priority = 1)
    public void testLoginPageLoads() {
        driver.get(BASE_URL + "/login");
        wait.until(ExpectedConditions.titleContains("ServiceHub"));
        assertTrue(driver.getPageSource().contains("ServiceHub"),
            "Login page should contain ServiceHub branding");
    }

    @Test(priority = 2)
    public void testUnauthenticatedRedirectsToLogin() {
        // Navigating to a protected route should redirect to /login
        driver.get(BASE_URL + "/dashboard");
        wait.until(ExpectedConditions.urlContains("/login"));
        assertTrue(driver.getCurrentUrl().contains("/login"),
            "Unauthenticated access to /dashboard should redirect to /login");
    }

    @Test(priority = 3)
    public void testLoginWithManagerCredentials() {
        driver.get(BASE_URL + "/login");
        // TODO: locate email and password fields once Angular components are built (#7)
        // WebElement emailField = wait.until(ExpectedConditions.elementToBeClickable(By.id("email")));
        // emailField.sendKeys("manager@amalitech.com");
        // driver.findElement(By.id("password")).sendKeys("password123");
        // driver.findElement(By.cssSelector("button[type=submit]")).click();
        // wait.until(ExpectedConditions.urlContains("/dashboard"));
        // assertTrue(driver.getCurrentUrl().contains("/dashboard"));
        System.out.println("STUB: testLoginWithManagerCredentials — unblock after #7 merges");
    }

    @Test(priority = 4)
    public void testSubmitRequestFlow() {
        // TODO: implement after Hassan's #9 (request submit component) merges
        System.out.println("STUB: testSubmitRequestFlow — unblock after #9 merges");
    }

    @Test(priority = 5)
    public void testStatusUpdateFlow() {
        // TODO: implement after Hassan's #8 (agent dashboard component) merges
        System.out.println("STUB: testStatusUpdateFlow — unblock after #8 merges");
    }

    @AfterClass
    public void teardown() {
        if (driver != null) driver.quit();
    }
}
