package com.amalitech.qa.pages;

import com.amalitech.qa.config.TestConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * Page Object for /dashboard (MANAGER view).
 * Localises selectors so AuthFlowTest never hard-codes DOM structure.
 */
public class ManagerDashboardPage extends BasePage {

    private static final String ROUTE = "/dashboard";

    /** Logout button — SVG-icon only (no text). Class from dashboard-shell.html. */
    private static final By LOGOUT_BTN = By.cssSelector("button.who-logout");

    public ManagerDashboardPage(WebDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        waitForUrl(ROUTE);
        // Wait for the shell name element — confirms Angular has bootstrapped
        // and (click) handlers are wired, not just that the URL changed.
        waitVisible(By.cssSelector("div.nm"));
        return isOnPage(ROUTE);
    }

    public void logout() {
        // JS click dispatches directly to the <button> element, bypassing
        // SVG hit-testing where a center-click can land on the inner path.
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click()", waitClickable(LOGOUT_BTN));
    }

    public void navigateTo() {
        driver.get(TestConfig.BASE_URL + ROUTE);
    }
}
