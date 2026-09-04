// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.thingsboard.server.msa.ui.utils.Const;

public class LoginPageHelper extends LoginPageElements {
    public LoginPageHelper(WebDriver driver) {
        super(driver);
    }

    public void authorizationTenant() {
        emailField().sendKeys(Const.TENANT_EMAIL);
        passwordField().sendKeys(Const.TENANT_PASSWORD);
        submitBtn().click();
        waitUntilUrlContainsText("/home");
    }
}
