// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.tabs;

import org.openqa.selenium.WebDriver;

public class ChangeDeviceOwnerTabHelper extends ChangeDeviceOwnerTabElements {
    public ChangeDeviceOwnerTabHelper(WebDriver driver) {
        super(driver);
    }

    public void selectOwner(String customerTitle) {
        changeOwnerField().sendKeys(customerTitle);
        if (changeOwnerField().getAttribute("value").isEmpty()) {
            changeOwnerField().sendKeys(customerTitle);
        }
        customerFromDropDown(customerTitle).click();
    }

    public void changeOwnerOn(String customerTitle) {
        selectOwner(customerTitle);
        changeOwnerBtn().click();
        yesBtnConfirmChangeOwner().click();
    }

}
