// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class UsersPageElements extends OtherPageElementsHelper {
    public UsersPageElements(WebDriver driver) {
        super(driver);
    }

    private static final String EMAILS = "//mat-cell[contains(@class,'cdk-column-column3')]/span";
    private static final String EMAIL = "//mat-cell[contains(@class,'cdk-column-column3')]/span[text() = '%s']";
    private static final String ALL_GROUP_NAMES = "//mat-cell[contains(@class,'cdk-column-name')]/span";
    private static final String ALL_NAMES = "//mat-cell[contains(@class,'cdk-column-column1')]/span";
    private static final String GROUPS_BTN = "//a[contains(@href,'/users/groups')]/span[@class='mdc-tab__content']";

    public List<WebElement> allGroupNames() {
        return waitUntilElementsToBeClickable(ALL_GROUP_NAMES);
    }

    public List<WebElement> allNames() {
        return waitUntilElementsToBeClickable(ALL_NAMES);
    }

    public List<WebElement> emails() {
        return waitUntilElementsToBeClickable(EMAILS);
    }

    public WebElement email(String email) {
        return waitUntilElementToBeClickable(String.format(EMAIL, email));
    }

    public WebElement groupsBtn() {
        return waitUntilElementToBeClickable(GROUPS_BTN);
    }
}
