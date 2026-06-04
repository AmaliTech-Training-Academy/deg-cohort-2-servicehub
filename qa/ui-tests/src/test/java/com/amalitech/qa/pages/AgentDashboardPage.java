package com.amalitech.qa.pages;

import com.amalitech.qa.config.TestConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Page Object for /agent-dashboard (AGENT view). */
public class AgentDashboardPage extends BasePage {

    private static final String ROUTE = "/agent-dashboard";

    private static final By LOGOUT_BTN = By.xpath(
            "//button[contains(text(),'Sign out') " +
            "or contains(text(),'Logout') " +
            "or contains(text(),'Log out')]");

    public AgentDashboardPage(WebDriver driver) {
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
