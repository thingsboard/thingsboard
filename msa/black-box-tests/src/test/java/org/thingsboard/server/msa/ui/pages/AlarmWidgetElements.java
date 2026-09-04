// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class AlarmWidgetElements extends AlarmDetailsEntityTabHelper {
    public AlarmWidgetElements(WebDriver driver) {
        super(driver);
    }

    private static final String ASSIGN_USER_DISPLAY_NAME = "//span[contains(@class,'assigned-container')]/span[contains(text(),'%s')]";
    private static final String UNASSIGNED = "//span[text() = '%s']/ancestor::mat-row//span[contains(@class,'assigned-container')]" +
            "//mat-icon[text() = 'account_circle']/following-sibling::span";

    @Override
    public WebElement assignedUser(String userEmail) {
        return waitUntilElementToBeClickable(String.format(ASSIGN_USER_DISPLAY_NAME, userEmail));
    }

    @Override
    public WebElement unassigned(String alarmType) {
        return waitUntilVisibilityOfElementLocated(String.format(UNASSIGNED, alarmType));
    }
}
