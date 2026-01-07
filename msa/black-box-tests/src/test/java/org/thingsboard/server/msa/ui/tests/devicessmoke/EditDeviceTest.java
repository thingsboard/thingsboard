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
package org.thingsboard.server.msa.ui.tests.devicessmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomNumber;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_DEVICE_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

@Feature("Edit device")
public class EditDeviceTest extends AbstractDeviceTest {

    @Test(groups = "smoke")
    @Description("Change name by edit menu")
    public void changeName() {
        String newDeviceName = "Changed" + getRandomNumber();
        deviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME)).getName();

        sideBarMenuView.goToDevicesPage();
        devicePage.entity(deviceName).click();
        devicePage.setHeaderName();
        String nameBefore = devicePage.getHeaderName();
        devicePage.editPencilBtn().click();
        devicePage.changeNameEditMenu(newDeviceName);
        devicePage.doneBtnEditView().click();
        deviceName = newDeviceName;
        devicePage.setHeaderName();
        String nameAfter = devicePage.getHeaderName();

        assertThat(nameAfter).as("The name has changed").isNotEqualTo(nameBefore);
        assertThat(nameAfter).as("The name has changed correctly").isEqualTo(newDeviceName);
    }

    @Test(groups = "smoke")
    @Description("Delete name and save")
    public void deleteName() {
        deviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME)).getName();

        sideBarMenuView.goToDevicesPage();
        devicePage.entity(deviceName).click();
        devicePage.editPencilBtn().click();
        devicePage.changeNameEditMenu("");

        assertIsDisable(devicePage.doneBtnEditViewVisible());
    }

    @Test(groups = "smoke")
    @Description("Save only with space")
    public void saveOnlyWithSpace() {
        deviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME)).getName();

        sideBarMenuView.goToDevicesPage();
        devicePage.entity(deviceName).click();
        devicePage.editPencilBtn().click();
        devicePage.changeNameEditMenu(" ");
        devicePage.doneBtnEditView().click();

        assertIsDisplayed(devicePage.warningMessage());
        assertThat(devicePage.warningMessage().getText()).as("Text of warning message").isEqualTo(EMPTY_DEVICE_MESSAGE);
    }

    @Test(groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "editMenuDescription")
    @Description("Write the description and save the changes/Change the description and save the changes/Delete the description and save the changes")
    public void editDescription(String description, String newDescription, String finalDescription) {
        deviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME, description)).getName();

        sideBarMenuView.goToDevicesPage();
        devicePage.entity(deviceName).click();
        devicePage.editPencilBtn().click();
        devicePage.descriptionEntityView().sendKeys(newDescription);
        devicePage.doneBtnEditView().click();
        devicePage.setDescription();

        assertThat(devicePage.getDescription()).as("The description changed correctly").isEqualTo(finalDescription);
    }

    @Test(groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "enable")
    @Description("Enable gateway mode/Disable gateway")
    public void isGateway(boolean isGateway) {
        deviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME, isGateway)).getName();

        sideBarMenuView.goToDevicesPage();
        devicePage.entity(deviceName).click();
        devicePage.editPencilBtn().click();
        devicePage.checkboxGatewayEdit().click();
        devicePage.doneBtnEditView().click();

        if (isGateway) {
            assertThat(devicePage.checkboxGatewayDetailsTab().getAttribute("class").contains("selected"))
                    .as("Gateway is disable").isFalse();
        } else {
            assertThat(devicePage.checkboxGatewayDetailsTab().getAttribute("class").contains("selected"))
                    .as("Gateway is enable").isTrue();
        }
    }

    @Test(groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "enable")
    @Description("Enable overwrite activity time for connected/Disable overwrite activity time for connected")
    public void isOverwriteActivityTimeForConnectedDevice(boolean isOverwriteActivityTimeForConnected) {
        deviceName = testRestClient.postDevice("",
                EntityPrototypes.defaultDevicePrototype(ENTITY_NAME, true, isOverwriteActivityTimeForConnected)).getName();

        sideBarMenuView.goToDevicesPage();
        devicePage.entity(deviceName).click();
        devicePage.editPencilBtn().click();
        devicePage.checkboxOverwriteActivityTimeEdit().click();
        devicePage.doneBtnEditView().click();

        if (isOverwriteActivityTimeForConnected) {
            assertThat(devicePage.checkboxOverwriteActivityTimeDetails().getAttribute("class").contains("selected"))
                    .as("Overwrite activity time for connected is disable").isFalse();
        } else {
            assertThat(devicePage.checkboxOverwriteActivityTimeDetails().getAttribute("class").contains("selected"))
                    .as("Overwrite activity time for connected is enable").isTrue();
        }
    }

    @Test(groups = "smoke")
    @Description("Change device profile")
    public void changeDeviceProfile() {
        deviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME)).getName();

        sideBarMenuView.goToDevicesPage();
        devicePage.entity(deviceName).click();
        devicePage.editPencilBtn().click();
        devicePage.changeDeviceProfile("DEFAULT");
        devicePage.doneBtnEditView().click();

        assertIsDisplayed(devicePage.deviceProfileRedirectedBtn());
        assertThat(devicePage.deviceProfileRedirectedBtn().getText()).as("Profile changed correctly").isEqualTo("DEFAULT");
    }

    @Test(groups = "smoke")
    @Description("Save without device profile")
    public void saveWithoutDeviceProfile() {
        deviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME)).getName();

        sideBarMenuView.goToDevicesPage();
        devicePage.entity(deviceName).click();
        devicePage.editPencilBtn().click();
        devicePage.clearProfileFieldBtn().click();

        assertIsDisable(devicePage.doneBtnEditViewVisible());
    }

    @Test(groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "editDeviceLabel")
    @Description("Write the label and save the changes/Change the label and save the changes/Delete the label and save the changes")
    public void editLabel(String label, String newLabel, String finalLabel) {
        deviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME, "", label)).getName();

        sideBarMenuView.goToDevicesPage();
        devicePage.entity(deviceName).click();
        devicePage.editPencilBtn().click();
        devicePage.deviceLabelEditField().sendKeys(newLabel);
        devicePage.doneBtnEditView().click();
        devicePage.setLabel();

        assertThat(devicePage.getLabel()).as("The label changed correctly").isEqualTo(finalLabel);
    }
}
