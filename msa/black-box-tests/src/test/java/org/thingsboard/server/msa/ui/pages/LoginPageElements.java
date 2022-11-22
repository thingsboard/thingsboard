package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.thingsboard.server.msa.ui.base.AbstractBasePage;

public class LoginPageElementsAbstract extends AbstractBasePage {
    public LoginPageElementsAbstract(WebDriver driver) {
        super(driver);
    }

    private static final String EMAIL_FIELD = "//input[@id='username-input']";
    private static final String PASSWORD_FIELD = "//input[@id='password-input']";
    private static final String SUBMIT_BTN = "//button[@type='submit']";

    public WebElement emailField() {
        return waitUntilElementToBeClickable(EMAIL_FIELD);
    }

    public WebElement passwordField() {
        return waitUntilElementToBeClickable(PASSWORD_FIELD);
    }

    public WebElement submitBtn() {
        return waitUntilElementToBeClickable(SUBMIT_BTN);
    }

}
