/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.msa.ui.tabs;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.thingsboard.server.msa.ui.base.AbstractBasePage;

public class CreateDeviceTabElements extends AbstractBasePage {
    public CreateDeviceTabElements(WebDriver driver) {
        super(driver);
    }

    private static final String CREATE_DEVICE_NAME_FIELD = "//tb-device-wizard//input[@formcontrolname='name']";
    private static final String CREATE_NEW_DEVICE_PROFILE_RADIO_BTN = "//span[text() = 'Create new device profile']/ancestor::mat-radio-button";
    private static final String SELECT_EXISTING_DEVICE_PROFILE_RADIO_BTN = "//span[text() = 'Select existing device profile']/ancestor::mat-radio-button";
    private static final String DEVICE_PROFILE_TITLE_FIELD = "//input[@formcontrolname='newDeviceProfileTitle']";
    private static final String ADD_BTN = "//span[text() = 'Add']";
    private static final String CLEAR_PROFILE_FIELD_BTN = "//button[@aria-label='Clear']";
    private static final String ENTITY_FROM_DROPDOWN = "//div[@role = 'listbox']//span[text() = '%s']";
    private static final String ASSIGN_ON_CUSTOMER_FIELD = "//input[@formcontrolname='entity']";
    private static final String CUSTOMER_OPTION_BNT = "//div[text() = 'Customer']/ancestor::mat-step-header";
    private static final String CUSTOMER_FROM_DROPDOWN = "//div[@role='listbox']/mat-option//span[contains(text(),'%s')]";
    private static final String DEVICE_LABEL_FIELD = "//tb-device-wizard//input[@formcontrolname='label']";
    private static final String CHECKBOX_GATEWAY = "//tb-device-wizard//mat-checkbox[@formcontrolname='gateway']//label";
    private static final String CHECKBOX_OVERWRITE_ACTIVITY_TIME = "//tb-device-wizard//mat-checkbox[@formcontrolname='overwriteActivityTime']//label";
    private static final String DESCRIPTION_FIELD = "//tb-device-wizard//textarea[@formcontrolname='description']";

    public WebElement nameField() {
        return waitUntilElementToBeClickable(CREATE_DEVICE_NAME_FIELD);
    }

    public WebElement createNewDeviceProfileRadioBtn() {
        return waitUntilElementToBeClickable(CREATE_NEW_DEVICE_PROFILE_RADIO_BTN);
    }

    public WebElement selectExistingDeviceProfileRadioBtn() {
        return waitUntilElementToBeClickable(SELECT_EXISTING_DEVICE_PROFILE_RADIO_BTN);
    }

    public WebElement deviceProfileTitleField() {
        return waitUntilElementToBeClickable(DEVICE_PROFILE_TITLE_FIELD);
    }

    public WebElement addBtn() {
        return waitUntilElementToBeClickable(ADD_BTN);
    }

    public WebElement clearProfileFieldBtn() {
        return waitUntilElementToBeClickable(CLEAR_PROFILE_FIELD_BTN);
    }

    public WebElement entityFromDropdown(String customerTitle) {
        return waitUntilElementToBeClickable(String.format(ENTITY_FROM_DROPDOWN, customerTitle));
    }

    public WebElement assignOnCustomerField() {
        return waitUntilElementToBeClickable(ASSIGN_ON_CUSTOMER_FIELD);
    }

    public WebElement customerOptionBtn() {
        return waitUntilElementToBeClickable(CUSTOMER_OPTION_BNT);
    }

    public WebElement customerFromDropDown(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(CUSTOMER_FROM_DROPDOWN, entityName));
    }

    public WebElement deviceLabelField() {
        return waitUntilElementToBeClickable(DEVICE_LABEL_FIELD);
    }

    public WebElement checkboxGateway() {
        return waitUntilElementToBeClickable(CHECKBOX_GATEWAY);
    }

    public WebElement checkboxOverwriteActivityTime() {
        return waitUntilElementToBeClickable(CHECKBOX_OVERWRITE_ACTIVITY_TIME);
    }

    public WebElement descriptionField() {
        return waitUntilElementToBeClickable(DESCRIPTION_FIELD);
    }
}
