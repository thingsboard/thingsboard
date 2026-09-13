// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
