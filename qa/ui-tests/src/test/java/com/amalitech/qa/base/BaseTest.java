package com.amalitech.qa.base;

import com.amalitech.qa.config.TestConfig;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.time.Duration;

/**
 * Driver lifecycle for all UI test classes.
 *
 * Extend this instead of duplicating setup/teardown in every test.
 * One ChromeDriver per class; localStorage cleared before each test method.
 */
public abstract class BaseTest {

    protected WebDriver driver;
    protected WebDriverWait wait;

    @BeforeClass
    public void setUpDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
        driver.manage().timeouts()
              .pageLoadTimeout(Duration.ofSeconds(TestConfig.PAGE_LOAD_TIMEOUT_SECONDS));
        wait = new WebDriverWait(driver,
                Duration.ofSeconds(TestConfig.WAIT_TIMEOUT_SECONDS));
    }

    @BeforeMethod
    public void clearSession() {
        driver.get(TestConfig.BASE_URL + "/login");
        ((JavascriptExecutor) driver).executeScript("localStorage.clear();");
    }

    @AfterClass(alwaysRun = true)
    public void tearDownDriver() {
        if (driver != null) {
            driver.quit();
        }
    }
}
