// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.tabs;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.thingsboard.server.msa.ui.base.AbstractBasePage;

public class ChangeDeviceOwnerTabElements extends AbstractBasePage {
    public ChangeDeviceOwnerTabElements(WebDriver driver) {
        super(driver);
    }

    private static final String CHANGE_OWNER_FIELD_FIELD = "//input[@formcontrolname='owner']";
    private static final String CUSTOMER_FROM_DROPDOWN = "//div[@role='listbox']/mat-option//b[contains(text(),'%s')]";
    private static final String CHANGE_OWNER_BTN = "//button[@type='submit']";
    private static final String YES_BTN_CONFIRM_CHANGE_OWNER = "//tb-confirm-dialog//span[text() = 'Yes']/parent::button";
    private static final String CLEAR_BTN = "//button[@aria-label='Clear']";
    private static final String ERROR_OWNER_REQUIRED = "//mat-error[contains(text(), 'Target owner is required')]";

    public WebElement changeOwnerField() {
        return waitUntilElementToBeClickable(CHANGE_OWNER_FIELD_FIELD);
    }

    public WebElement customerFromDropDown(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(CUSTOMER_FROM_DROPDOWN, entityName));
    }

    public WebElement changeOwnerBtn() {
        return waitUntilElementToBeClickable(CHANGE_OWNER_BTN);
    }

    public WebElement changeOwnerBtnVisible() {
        return waitUntilVisibilityOfElementLocated(CHANGE_OWNER_BTN);
    }

    public WebElement yesBtnConfirmChangeOwner() {
        return waitUntilElementToBeClickable(YES_BTN_CONFIRM_CHANGE_OWNER);
    }

    public WebElement clearBtn() {
        return waitUntilElementToBeClickable(CLEAR_BTN);
    }

    public WebElement errorOwnerRequired() {
        return waitUntilVisibilityOfElementLocated(ERROR_OWNER_REQUIRED);
    }
}
