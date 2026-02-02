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
