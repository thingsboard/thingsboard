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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

@Feature("Filter devices (By device profile and state)")
public class DeviceFilterTest extends AbstractDeviceTest {

    private String activeDeviceName;
    private String deviceWithProfileName;
    private String activeDeviceWithProfileName;

    @BeforeClass
    public void createTestEntities() {
        DeviceProfile deviceProfile = testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(ENTITY_NAME + random()));
        Device deviceWithProfile = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME + random(), deviceProfile.getId()));
        Device activeDevice = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME + random()));
        Device activeDeviceWithProfile = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME + random(), deviceProfile.getId()));

        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(activeDevice.getId());
        DeviceCredentials deviceCredentials1 = testRestClient.getDeviceCredentialsByDeviceId(activeDeviceWithProfile.getId());
        testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), JacksonUtil.toJsonNode(createPayload().toString()));
        testRestClient.postTelemetry(deviceCredentials1.getCredentialsId(), JacksonUtil.toJsonNode(createPayload().toString()));

        deviceProfileTitle = deviceProfile.getName();
        deviceWithProfileName = deviceWithProfile.getName();
        activeDeviceName = activeDevice.getName();
        activeDeviceWithProfileName = activeDeviceWithProfile.getName();
    }

    @AfterClass
    public void deleteTestEntities() {
        deleteDevicesByName(List.of(deviceWithProfileName, activeDeviceName, activeDeviceWithProfileName));
        deleteDeviceProfileByTitle(deviceProfileTitle);
    }

    @Test(groups = "smoke")
    @Description("Filter by device profile")
    public void filterDevicesByProfile() {
        sideBarMenuView.goToDevicesPage();
        devicePage.filterBtn().click();
        devicePage.filterDeviceByDeviceProfile(deviceProfileTitle);

        devicePage.listOfDevicesProfile().forEach(d -> assertThat(d.getText())
                .as("There are only devices with the selected profile(%s) on the page", deviceProfileTitle)
                .isEqualTo(deviceProfileTitle));
    }

    @Test(groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "filterData")
    @Description("Filter by state")
    public void filterDevicesByState(String state) {
        sideBarMenuView.goToDevicesPage();
        devicePage.filterBtn().click();
        devicePage.filterDeviceByState(state);

        devicePage.listOfDevicesState().forEach(d -> assertThat(d.getText())
                .as("There are only devices with '%s' state on the page", state)
                .isEqualTo(state));
    }

    @Test(groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "filterData")
    @Description("Filter device by device profile and state")
    public void filterDevicesByDeviceProfileAndState(String state) {
        sideBarMenuView.goToDevicesPage();
        devicePage.filterBtn().click();
        devicePage.filterDeviceByDeviceProfileAndState(deviceProfileTitle, state);

        devicePage.listOfDevicesProfile().forEach(d -> assertThat(d.getText())
                .as("There are only devices with the selected profile(%s) on the page", deviceProfileTitle)
                .isEqualTo(deviceProfileTitle));
        devicePage.listOfDevicesState().forEach(d -> assertThat(d.getText())
                .as("There are only devices with '%s' state on the page", state)
                .isEqualTo(state));
    }
}
