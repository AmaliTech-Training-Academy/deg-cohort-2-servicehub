package com.amalitech.qa.pages;

import com.amalitech.qa.config.TestConfig;
import org.openqa.selenium.By;
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
        return isOnPage(ROUTE);
    }

    public void logout() {
        waitClickable(LOGOUT_BTN).click();
    }

    public void navigateTo() {
        driver.get(TestConfig.BASE_URL + ROUTE);
    }
}
