package com.amalitech.qa.pages;

import com.amalitech.qa.config.TestConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/** Page Object for /agent-dashboard (AGENT view). */
public class AgentDashboardPage extends BasePage {

    private static final String ROUTE = "/agent-dashboard";

    /** Logout button — SVG-icon only (no text). Class from dashboard-shell.html. */
    private static final By LOGOUT_BTN = By.cssSelector("button.who-logout");

    public AgentDashboardPage(WebDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        waitForUrl(ROUTE);
        waitVisible(By.cssSelector("div.nm"));
        return isOnPage(ROUTE);
    }

    public void logout() {
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click()", waitClickable(LOGOUT_BTN));
    }

    public void navigateTo() {
        driver.get(TestConfig.BASE_URL + ROUTE);
    }
}
