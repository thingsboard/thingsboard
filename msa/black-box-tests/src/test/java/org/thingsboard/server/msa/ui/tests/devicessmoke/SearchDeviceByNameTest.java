/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomNumber;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

@Feature("Search devices by name")
public class SearchDeviceByNameTest extends AbstractDeviceTest {
    private Device device;
    private Device device1;
    private Device device2;

    @BeforeClass
    public void create() {
        device = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("Things Board"));
        device1 = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME));
        device2 = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(getRandomNumber() + " "));
    }

    @AfterClass
    public void delete() {
        deleteDeviceByName(device.getName());
        deleteDeviceByName(device1.getName());
        deleteDeviceByName(device2.getName());
    }

    @Test(dataProviderClass = DataProviderCredential.class, dataProvider = "devicesSearchPath")
    @Description("Search device by first word, by second word, by symbol in the name")
    public void searchDeviceByName(String namePath) {
        sideBarMenuView.goToDevicesPage();
        devicePage.searchEntity(namePath);

        assertListOfElementContainsText(devicePage.allEntity(), namePath);
    }

    @Test
    @Description("Search device by number in the name")
    public void searchDeviceByNameByNumber() {
        sideBarMenuView.goToDevicesPage();
        devicePage.searchEntity(device2.getName().split(" ")[0]);

        assertListOfElementContainsText(devicePage.allEntity(), device2.getName());
    }
}
