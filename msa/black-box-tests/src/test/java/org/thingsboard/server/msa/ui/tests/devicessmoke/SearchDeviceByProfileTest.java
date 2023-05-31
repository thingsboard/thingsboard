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
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomNumber;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

@Feature("Search devices by profile")
public class SearchDeviceByProfileTest extends AbstractDeviceTest {

    private Device deviceWithProfile;
    private Device deviceWithProfile1;
    private Device deviceWithProfile2;
    private DeviceProfile deviceProfile;
    private DeviceProfile deviceProfile1;
    private DeviceProfile deviceProfile2;

    @BeforeClass
    public void create() {
        deviceProfile = testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(ENTITY_NAME + random()));
        deviceProfile1 = testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile("Things Board" + random()));
        deviceProfile2 = testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(getRandomNumber()));

        deviceWithProfile = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("Device", deviceProfile.getId()));
        deviceWithProfile1 = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("Device", deviceProfile1.getId()));
        deviceWithProfile2 = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("Device", deviceProfile2.getId()));
    }

    @AfterClass
    public void delete() {
        deleteDeviceByName(deviceWithProfile.getName());
        deleteDeviceByName(deviceWithProfile1.getName());
        deleteDeviceByName(deviceWithProfile2.getName());
        deleteDeviceProfileByTitle(deviceProfile.getName());
        deleteDeviceProfileByTitle(deviceProfile1.getName());
        deleteDeviceProfileByTitle(deviceProfile2.getName());
    }

    @Test(dataProviderClass = DataProviderCredential.class, dataProvider = "devicesSearchPath")
    @Description("Search device by first word, by second word, by symbol in the profile's name")
    public void searchDeviceByProfile(String namePath) {
        sideBarMenuView.goToDevicesPage();
        devicePage.searchEntity(namePath);

        assertListOfElementContainsText(devicePage.listOfDevicesProfileCells(), namePath);
    }

    @Test
    @Description("Search device by number in the profile's name")
    public void searchDeviceByProfileByNumber() {
        sideBarMenuView.goToDevicesPage();
        devicePage.searchEntity(deviceProfile2.getName());

        assertListOfElementContainsText(devicePage.listOfDevicesProfileCells(), deviceProfile2.getName());
    }
}
