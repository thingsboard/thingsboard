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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

@Feature("Delete device")
public class DeleteDeviceTest extends AbstractDeviceTest {

    @BeforeMethod
    public void createDevice() {
        Device device = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME));
        deviceName = device.getName();
    }

    @Test(groups = "smoke")
    @Description("Remove the device by clicking on the trash icon in the right side of device")
    public void deleteDeviceByRightSideBtn() {
        sideBarMenuView.goToDevicesPage();
        devicePage.deleteDeviceByRightSideBtn(deviceName);
        devicePage.refreshBtn().click();

        devicePage.assertEntityIsNotPresent(deviceName);
    }

    @Test(groups = "smoke")
    @Description("Remove device by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void deleteSelectedDevice() {
        sideBarMenuView.goToDevicesPage();
        devicePage.deleteSelected(deviceName);
        devicePage.refreshBtn().click();

        devicePage.assertEntityIsNotPresent(deviceName);
    }

    @Test(groups = "smoke")
    @Description("Remove the device by clicking on the 'Delete device' btn in the entity view")
    public void deleteDeviceFromDetailsTab() {
        sideBarMenuView.goToDevicesPage();
        devicePage.entity(deviceName).click();
        devicePage.deleteDeviceFromDetailsTab();
        devicePage.refreshBtn();

        devicePage.assertEntityIsNotPresent(deviceName);
    }

    @Test(groups = "smoke")
    @Description("Remove the device by clicking on the trash icon in the right side of device without refresh")
    public void deleteDeviceWithoutRefresh() {
        sideBarMenuView.goToDevicesPage();
        devicePage.deleteDeviceByRightSideBtn(deviceName);

        devicePage.assertEntityIsNotPresent(deviceName);
    }
}
