/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.thingsboard.server.msa.ui.utils.Const;

public class LoginPageHelper extends LoginPageElements {
    public LoginPageHelper(WebDriver driver) {
        super(driver);
    }

    public void login(String username, String password) {
        emailField().sendKeys(username);
        passwordField().sendKeys(password);
        submitBtn().click();
        waitUntilUrlContainsText("/home");
    }

    public void authorizationTenant() {
        login(Const.TENANT_EMAIL, Const.TENANT_PASSWORD);
    }

    public void authorizationCustomer() {
        login("customer@thingsboard.org", "customer");
    }
}
