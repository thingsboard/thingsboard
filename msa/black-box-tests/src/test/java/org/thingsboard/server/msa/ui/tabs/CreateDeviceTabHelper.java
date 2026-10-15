// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.tabs;

import org.openqa.selenium.WebDriver;

public class CreateDeviceTabHelper extends CreateDeviceTabElements {
    public CreateDeviceTabHelper(WebDriver driver) {
        super(driver);
    }

    public void enterName(String deviceName) {
        enterText(nameField(), deviceName);
    }

    public void createNewDeviceProfile(String deviceProfileTitle) {
        if (!createNewDeviceProfileRadioBtn().getAttribute("class").contains("checked")) {
            createNewDeviceProfileRadioBtn().click();
        }
        deviceProfileTitleField().sendKeys(deviceProfileTitle);
    }

    public void changeDeviceProfile(String deviceProfileName) {
        if (!selectExistingDeviceProfileRadioBtn().getAttribute("class").contains("checked")) {
            selectExistingDeviceProfileRadioBtn().click();
        }
        clearProfileFieldBtn().click();
        entityFromDropdown(deviceProfileName).click();
    }

    public void assignOnCustomer(String customerTitle) {
        customerOptionBtn().click();
        assignOnCustomerField().click();
        customerFromDropDown(customerTitle).click();
        sleep(2); //waiting for the action to count
    }

    public void enterLabel(String label) {
        enterText(deviceLabelField(), label);
    }

    public void enterDescription(String description) {
        enterText(descriptionField(), description);
    }
}
