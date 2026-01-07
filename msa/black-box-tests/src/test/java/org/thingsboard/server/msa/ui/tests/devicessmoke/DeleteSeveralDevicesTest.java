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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

@Feature("Delete several devices")
public class DeleteSeveralDevicesTest extends AbstractDeviceTest {

    private String deviceName1;
    private String deviceName2;

    @BeforeMethod
    public void createDevices() {
        Device device = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME));
        Device device1 = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME));
        deviceName1 = device.getName();
        deviceName2 = device1.getName();
    }

    @AfterMethod
    public void deleteDevices() {
        deleteDeviceByName(deviceName1);
        deleteDeviceByName(deviceName2);
    }

    @Test(groups = "smoke")
    @Description("Remove several devices by mark in the checkbox and then click on the trash can icon in the menu " +
            "that appears at the top")
    public void deleteSeveralDevicesByTopBtn() {
        sideBarMenuView.goToDevicesPage();
        devicePage.deleteSelectedDevices(deviceName1, deviceName2);
        devicePage.refreshBtn().click();

        List.of(deviceName1, deviceName2)
                .forEach(d -> devicePage.assertEntityIsNotPresent(d));
    }

    @Test(groups = "smoke")
    @Description("Remove several devices by mark all the devices on the page by clicking in the topmost checkbox" +
            " and then clicking on the trash icon in the menu that appears")
    public void selectAllDevices() {
        sideBarMenuView.goToDevicesPage();
        devicePage.selectAllCheckBox().click();
        devicePage.deleteSelectedBtn().click();

        assertIsDisplayed(devicePage.warningPopUpTitle());
        assertThat(devicePage.warningPopUpTitle().getText()).as("Warning title contains true correct of selected devices")
                .contains(String.valueOf(devicePage.markCheckbox().size()));
    }

    @Test(groups = "smoke")
    @Description("Remove several devices by mark in the checkbox and then click on the trash can icon in the menu " +
            "that appears at the top without refresh")
    public void deleteSeveralWithoutRefresh() {
        sideBarMenuView.goToDevicesPage();
        devicePage.deleteSelectedDevices(deviceName1, deviceName2);

        List.of(deviceName1, deviceName2)
                .forEach(d -> devicePage.assertEntityIsNotPresent(d));
    }
}
