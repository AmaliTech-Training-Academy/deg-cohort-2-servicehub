package com.amalitech.qa.base;

import com.amalitech.qa.config.TestConfig;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.time.Duration;

/**
 * Driver lifecycle for all UI test classes.
 *
 * A fresh ChromeDriver is created for every test method — this eliminates
 * cross-test session accumulation that causes flakes when 13 login flows
 * run in a shared long-lived browser instance.
 *
 * The extra cost is ~1s per browser start; acceptable for a 13-test suite.
 */
public abstract class BaseTest {

    protected WebDriver driver;
    protected WebDriverWait wait;

    @BeforeSuite
    public void setUpDriverManager() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeMethod
    public void startDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--headless",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--window-size=1920,1080"
        );
        driver = new ChromeDriver(options);
        driver.manage().timeouts()
              .pageLoadTimeout(Duration.ofSeconds(TestConfig.PAGE_LOAD_TIMEOUT_SECONDS));
        wait = new WebDriverWait(driver,
                Duration.ofSeconds(TestConfig.WAIT_TIMEOUT_SECONDS));

        // Navigate to login and wait for Angular to initialise before the test starts
        driver.get(TestConfig.BASE_URL + "/login");
        new WebDriverWait(driver, Duration.ofSeconds(TestConfig.WAIT_TIMEOUT_SECONDS))
                .until(ExpectedConditions.urlContains("/login"));
        ((JavascriptExecutor) driver)
                .executeScript("localStorage.clear(); sessionStorage.clear();");
    }

    @AfterMethod(alwaysRun = true)
    public void quitDriver() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }
}
