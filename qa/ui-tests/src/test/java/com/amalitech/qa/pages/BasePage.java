package com.amalitech.qa.pages;

import com.amalitech.qa.config.TestConfig;
import org.openqa.selenium.*;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Base for all page objects.
 * Provides typed waits and navigation helpers — no test logic here.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait   = new WebDriverWait(driver,
                Duration.ofSeconds(TestConfig.WAIT_TIMEOUT_SECONDS));
        PageFactory.initElements(driver, this);
    }

    /** Wait until the URL contains the given fragment. */
    public void waitForUrl(String fragment) {
        wait.until(ExpectedConditions.urlContains(fragment));
    }

    public String currentUrl() {
        return driver.getCurrentUrl();
    }

    public boolean isOnPage(String urlFragment) {
        return driver.getCurrentUrl().contains(urlFragment);
    }

    /** Clear localStorage — call between tests to simulate fresh sessions. */
    public void clearSession() {
        ((JavascriptExecutor) driver).executeScript("localStorage.clear();");
    }

    /** Plant a JWT directly into localStorage without going through the login UI. */
    public void plantToken(String key, String value) {
        ((JavascriptExecutor) driver)
                .executeScript("localStorage.setItem(arguments[0], arguments[1]);", key, value);
    }

    /** Wait for an element to be visible and return it. */
    protected WebElement waitVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** Wait for an element to be clickable and return it. */
    protected WebElement waitClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    /** Returns true if the element matching the locator is currently visible. */
    protected boolean isVisible(By locator) {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator))
                       .isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** True when no element matches the locator. */
    protected boolean isAbsent(By locator) {
        return driver.findElements(locator).isEmpty();
    }
}
