package com.amalitech.qa.pages;

import com.amalitech.qa.config.TestConfig;
import com.amalitech.qa.data.TestUsers;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Page Object for /login.
 *
 * Locators: IDs preferred (id="email", id="password") — most stable.
 * The error selector (p.text-red-600) targets the Tailwind error message
 * introduced in PR #84 (login redesign). Verify this selector if the
 * login component is further modified.
 */
public class LoginPage extends BasePage {

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(css = "button[type=submit]")
    private WebElement submitButton;

    /** Error paragraph rendered on invalid credentials. */
    private static final By ERROR_SELECTOR = By.cssSelector("p.text-red-600");

    // ── Construction / navigation ─────────────────────────────────────────────

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage open() {
        driver.get(TestConfig.BASE_URL + "/login");
        waitClickable(By.id("email"));
        return this;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public void enterEmail(String email) {
        emailInput.clear();
        emailInput.sendKeys(email);
    }

    public void enterPassword(String password) {
        passwordInput.clear();
        passwordInput.sendKeys(password);
    }

    public void clickSubmit() {
        submitButton.click();
    }

    /**
     * Full login flow — navigates to /login, fills form, submits.
     * Does NOT wait for redirect; callers own the assertion.
     */
    public void login(TestUsers.Credentials user) {
        open();
        enterEmail(user.email());
        enterPassword(user.password());
        clickSubmit();
    }

    /** Login with arbitrary credentials (negative tests). */
    public void login(String email, String password) {
        open();
        enterEmail(email);
        enterPassword(password);
        clickSubmit();
    }

    // ── State queries ─────────────────────────────────────────────────────────

    public boolean isOnLoginPage() {
        return isOnPage("/login");
    }

    public boolean isErrorVisible() {
        return isVisible(ERROR_SELECTOR);
    }

    public String getErrorText() {
        WebElement el = waitVisible(ERROR_SELECTOR);
        return el.getText();
    }

    public boolean isErrorAbsent() {
        return isAbsent(ERROR_SELECTOR);
    }
}
