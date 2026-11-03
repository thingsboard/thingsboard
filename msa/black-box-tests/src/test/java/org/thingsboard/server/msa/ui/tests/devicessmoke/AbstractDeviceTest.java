// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.tests.devicessmoke;

import io.qameta.allure.Epic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.DevicePageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.tabs.CreateDeviceTabHelper;

@Epic("Device smoke tests")
abstract public class AbstractDeviceTest extends AbstractDriverBaseTest {

    protected SideBarMenuViewHelper sideBarMenuView;
    protected DevicePageHelper devicePage;
    protected CreateDeviceTabHelper createDeviceTab;
    protected String deviceName;
    protected String deviceProfileTitle;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        devicePage = new DevicePageHelper(driver);
        createDeviceTab = new CreateDeviceTabHelper(driver);
    }

    @AfterMethod
    public void delete() {
        deleteDeviceByName(deviceName);
        deviceName = null;
    }
}
