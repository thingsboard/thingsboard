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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.msa.ui.pages.ProfilesPageElements;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.DEVICE_PROFILE_IS_REQUIRED_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_DEVICE_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.NAME_IS_REQUIRED_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.SAME_NAME_WARNING_DEVICE_MESSAGE;

@Feature("Create device")
public class CreateDeviceTest extends AbstractDeviceTest {

    @AfterMethod
    public void delete() {
        deleteDeviceByName(deviceName);
        deviceName = null;
        if (deviceProfileTitle != null) {
            deleteDeviceProfileByTitle(deviceProfileTitle);
            deviceProfileTitle = null;
        }
    }

    @Test(groups = "smoke")
    @Description("Add device after specifying the name (text/numbers /special characters)")
    public void createDevice() {
        deviceName = ENTITY_NAME + random();

        sideBarMenuView.goToDevicesPage();
        devicePage.openCreateDeviceView();
        createDeviceTab.enterName(deviceName);
        createDeviceTab.addBtn().click();
        devicePage.refreshBtn().click();

        assertIsDisplayed(devicePage.entity(deviceName));
    }

    @Test(groups = "smoke")
    @Description("Add device after specifying the name and description (text/numbers /special characters)")
    public void createDeviceWithDescription() {
        deviceName = ENTITY_NAME + random();

        sideBarMenuView.goToDevicesPage();
        devicePage.openCreateDeviceView();
        createDeviceTab.enterName(deviceName);
        createDeviceTab.enterDescription(deviceName);
        createDeviceTab.addBtn().click();
        devicePage.refreshBtn().click();
        devicePage.entity(deviceName).click();
        devicePage.setHeaderName();

        assertThat(devicePage.getHeaderName()).as("Header of device details tab").isEqualTo(deviceName);
        assertThat(devicePage.descriptionEntityView().getAttribute("value"))
                .as("Description in device details tab").isEqualTo(deviceName);
    }

    @Test(groups = "smoke")
    @Description("Add device without the name")
    public void createDeviceWithoutName() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openCreateDeviceView();
        createDeviceTab.nameField().click();
        createDeviceTab.addBtn().click();

        assertIsDisplayed(devicePage.addDeviceView());
        assertThat(devicePage.errorMessage().getText()).as("Text of warning message").isEqualTo(NAME_IS_REQUIRED_MESSAGE);
    }

    @Test(groups = "smoke")
    @Description("Create device only with spase in name")
    public void createDeviceWithOnlySpace() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openCreateDeviceView();
        createDeviceTab.enterName(" ");
        createDeviceTab.addBtn().click();

        assertIsDisplayed(devicePage.warningMessage());
        assertThat(devicePage.warningMessage().getText()).as("Text of warning message").isEqualTo(EMPTY_DEVICE_MESSAGE);
        assertIsDisplayed(devicePage.addDeviceView());
    }

    @Test(groups = "smoke")
    @Description("Create a device with the same name")
    public void createDeviceWithSameName() {
        Device device = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME));
        deviceName = device.getName();

        sideBarMenuView.goToDevicesPage();
        devicePage.openCreateDeviceView();
        createDeviceTab.enterName(deviceName);
        createDeviceTab.addBtn().click();

        assertIsDisplayed(devicePage.warningMessage());
        assertThat(devicePage.warningMessage().getText()).as("Text of warning message").isEqualTo(SAME_NAME_WARNING_DEVICE_MESSAGE);
        assertIsDisplayed(devicePage.addDeviceView());
    }

    @Test(groups = "smoke")
    @Description("Add device after specifying the name (text/numbers /special characters) without refresh")
    public void createDeviceWithoutRefresh() {
        deviceName = ENTITY_NAME + random();

        sideBarMenuView.goToDevicesPage();
        devicePage.openCreateDeviceView();
        createDeviceTab.enterName(deviceName);
        createDeviceTab.addBtn().click();

        assertIsDisplayed(devicePage.entity(deviceName));
    }

    @Test(groups = "smoke")
    @Description("Add device without device profile")
    public void createDeviceWithoutDeviceProfile() {
        deviceName = ENTITY_NAME + random();

        sideBarMenuView.goToDevicesPage();
        devicePage.openCreateDeviceView();
        createDeviceTab.enterName(deviceName);
        createDeviceTab.clearProfileFieldBtn().click();
        createDeviceTab.addBtn().click();

        assertIsDisplayed(devicePage.errorMessage());
        assertThat(devicePage.errorMessage().getText()).as("Text of warning message").isEqualTo(DEVICE_PROFILE_IS_REQUIRED_MESSAGE);
        assertIsDisplayed(devicePage.addDeviceView());
    }

    @Test(groups = "smoke")
    @Description("Add device with enabled gateway")
    public void createDeviceWithEnableGateway() {
        deviceName = ENTITY_NAME + random();

        sideBarMenuView.goToDevicesPage();
        devicePage.openCreateDeviceView();
        createDeviceTab.enterName(deviceName);
        createDeviceTab.checkboxGateway().click();
        createDeviceTab.addBtn().click();

        assertIsDisplayed(devicePage.device(deviceName));
        assertIsDisplayed(devicePage.checkboxGatewayPage(deviceName));
    }

    @Test(groups = "smoke")
    @Description("Add device with enabled overwrite activity time for connected")
    public void createDeviceWithEnableOverwriteActivityTimeForConnected() {
        deviceName = ENTITY_NAME + random();

        sideBarMenuView.goToDevicesPage();
        devicePage.openCreateDeviceView();
        createDeviceTab.enterName(deviceName);
        createDeviceTab.checkboxGateway().click();
        createDeviceTab.checkboxOverwriteActivityTime().click();
        createDeviceTab.addBtn().click();
        devicePage.device(deviceName).click();

        assertThat(devicePage.checkboxOverwriteActivityTimeDetails().getAttribute("class").contains("selected"))
                .as("Overwrite activity time for connected is enable").isTrue();
    }

    @Test(groups = "smoke")
    @Description("Add device with label")
    public void createDeviceWithLabel() {
        deviceName = ENTITY_NAME + random();
        String deviceLabel = "device label " + random();

        sideBarMenuView.goToDevicesPage();
        devicePage.openCreateDeviceView();
        createDeviceTab.enterName(deviceName);
        createDeviceTab.enterLabel(deviceLabel);
        createDeviceTab.addBtn().click();

        assertIsDisplayed(devicePage.deviceLabelOnPage(deviceName));
        assertThat(devicePage.deviceLabelOnPage(deviceName).getText()).as("Label added correctly").isEqualTo(deviceLabel);
    }

    @Test(groups = "smoke")
    @Description("Add device with assignee on customer")
    public void createDeviceWithAssignee() {
        deviceName = ENTITY_NAME + random();
        String customer = "Customer A";

        sideBarMenuView.goToDevicesPage();
        devicePage.openCreateDeviceView();
        createDeviceTab.enterName(deviceName);
        createDeviceTab.assignOnCustomer(customer);
        createDeviceTab.addBtn().click();

        assertIsDisplayed(devicePage.deviceCustomerOnPage(deviceName));
        assertThat(devicePage.deviceCustomerOnPage(deviceName).getText())
                .as("Customer added correctly").isEqualTo(customer);
    }

    @Test(groups = "smoke")
    @Description("Go to devices documentation page")
    public void documentation() {
        String urlPath = "docs/user-guide/ui/devices/";

        sideBarMenuView.goToDevicesPage();
        devicePage.entity("Thermostat T1").click();
        devicePage.goToHelpPage();

        assertThat(urlContains(urlPath)).as("Redirected URL contains " + urlPath).isTrue();
    }

    @Test(groups = "smoke")
    @Description("Create new device profile from create device")
    public void createNewDeviceProfile() {
        ProfilesPageElements profilesPage = new ProfilesPageElements(driver);
        deviceName = ENTITY_NAME + random();
        deviceProfileTitle = ENTITY_NAME + random();

        sideBarMenuView.goToDevicesPage();
        devicePage.openCreateDeviceView();
        createDeviceTab.enterName(deviceName);
        createDeviceTab.createNewDeviceProfile(deviceProfileTitle);
        createDeviceTab.addBtn().click();
        devicePage.refreshBtn().click();
        String deviceProfileColumn = devicePage.deviceDeviceProfileOnPage(deviceName).getText();
        sideBarMenuView.openDeviceProfiles();

        assertThat(deviceProfileColumn).as("Profile changed correctly").isEqualTo(deviceProfileTitle);
        assertIsDisplayed(profilesPage.entity(deviceProfileTitle));
    }

    @Test(groups = "smoke")
    @Description("Add device with changed device profile (from default to another)")
    public void createDeviceWithChangedProfile() {
        deviceName = ENTITY_NAME + random();
        deviceProfileTitle = ENTITY_NAME + random();
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(deviceProfileTitle));

        sideBarMenuView.goToDevicesPage();
        devicePage.openCreateDeviceView();
        createDeviceTab.enterName(deviceName);
        createDeviceTab.changeDeviceProfile(deviceProfileTitle);
        createDeviceTab.addBtn().click();
        devicePage.refreshBtn().click();

        assertThat(devicePage.deviceDeviceProfileOnPage(deviceName).getText())
                .as("Profile changed correctly").isEqualTo(deviceProfileTitle);
    }
}
