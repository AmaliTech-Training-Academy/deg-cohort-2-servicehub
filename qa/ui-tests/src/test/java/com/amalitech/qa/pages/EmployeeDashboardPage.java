package com.amalitech.qa.pages;

import com.amalitech.qa.config.TestConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Page Object for /my-dashboard (EMPLOYEE view). */
public class EmployeeDashboardPage extends BasePage {

    private static final String ROUTE = "/my-dashboard";

    /** Logout button — SVG-icon only (no text). Class from dashboard-shell.html. */
    private static final By LOGOUT_BTN = By.cssSelector("button.who-logout");

    public EmployeeDashboardPage(WebDriver driver) {
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
