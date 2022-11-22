package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.thingsboard.server.msa.ui.utils.Const;

public class LoginPageHelperAbstract extends LoginPageElementsAbstract {
    public LoginPageHelperAbstract(WebDriver driver) {
        super(driver);
    }

    public void authorizationTenant() {
        emailField().sendKeys(Const.TENANT_EMAIL);
        passwordField().sendKeys(Const.TENANT_PASSWORD);
        submitBtn().click();
    }
}
