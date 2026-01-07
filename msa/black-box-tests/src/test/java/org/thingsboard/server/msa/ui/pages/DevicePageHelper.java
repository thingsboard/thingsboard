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
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;

public class DevicePageHelper extends DevicePageElements {
    public DevicePageHelper(WebDriver driver) {
        super(driver);
    }

    private String description;
    private String label;

    public void openDeviceAlarms(String deviceName) {
        if (!deviceDetailsView().isDisplayed()) {
            device(deviceName).click();
        }
        deviceDetailsAlarmsBtn().click();
    }

    public void assignToCustomer(String customerTitle) {
        chooseCustomerForAssignField().click();
        entityFromDropdown(customerTitle).click();
        submitBtn().click();
    }

    public void openCreateDeviceView() {
        plusBtn().click();
        addDeviceBtn().click();
    }

    public void deleteDeviceByRightSideBtn(String deviceName) {
        deleteBtn(deviceName).click();
        warningPopUpYesBtn().click();
    }

    public void deleteDeviceFromDetailsTab() {
        deleteBtnDetailsTab().click();
        warningPopUpYesBtn().click();
    }

    public void setDescription() {
        scrollToElement(descriptionEntityView());
        description = descriptionEntityView().getAttribute("value");
    }

    public void setLabel() {
        label = deviceLabelDetailsField().getAttribute("value");
    }

    public String getDescription() {
        return description;
    }

    public String getLabel() {
        return label;
    }

    public void changeDeviceProfile(String deviceProfileName) {
        clearProfileFieldBtn().click();
        entityFromDropdown(deviceProfileName).click();
    }

    public void unassignedDeviceByRightSideBtn(String deviceName) {
        unassignBtn(deviceName).click();
        warningPopUpYesBtn().click();
    }

    public void unassignedDeviceFromDetailsTab() {
        unassignBtnDetailsTab().click();
        warningPopUpYesBtn().click();
    }

    public void selectDevices(String... deviceNames) {
        for (String deviceName : deviceNames) {
            checkBox(deviceName).click();
        }
    }

    public void assignSelectedDevices(String... deviceNames) {
        selectDevices(deviceNames);
        assignMarkedDeviceBtn().click();
    }

    public void deleteSelectedDevices(String... deviceNames) {
        selectDevices(deviceNames);
        deleteSelectedBtn().click();
        warningPopUpYesBtn().click();
    }

    public void filterDeviceByDeviceProfile(String deviceProfileTitle) {
        clearProfileFieldBtn().click();
        entityFromDropdown(deviceProfileTitle).click();
        submitBtn().click();
    }

    public void filterDeviceByState(String state) {
        deviceStateSelect().click();
        entityFromDropdown(" " + state + " ").click();
        sleep(2); //wait until the action is counted
        submitBtn().click();
    }

    public void filterDeviceByDeviceProfileAndState(String deviceProfileTitle, String state) {
        clearProfileFieldBtn().click();
        entityFromDropdown(deviceProfileTitle).click();
        deviceStateSelect().click();
        entityFromDropdown(" " + state + " ").click();
        sleep(2); //wait until the action is counted
        submitBtn().click();
    }

    public void makeDevicePublicByRightSideBtn(String deviceName) {
        makeDevicePublicBtn(deviceName).click();
        warningPopUpYesBtn().click();
    }

    public void makeDevicePublicFromDetailsTab() {
        makeDevicePublicBtnDetailsTab().click();
        warningPopUpYesBtn().click();
    }

    public void makeDevicePrivateByRightSideBtn(String deviceName) {
        makeDevicePrivateBtn(deviceName).click();
        warningPopUpYesBtn().click();
    }

    public void makeDevicePrivateFromDetailsTab() {
        makeDevicePrivateBtnDetailsTab().click();
        warningPopUpYesBtn().click();
    }
}
