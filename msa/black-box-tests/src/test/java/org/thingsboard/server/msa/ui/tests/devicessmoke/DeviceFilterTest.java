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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.DEVICE_ACTIVE_STATE;
import static org.thingsboard.server.msa.ui.utils.Const.DEVICE_INACTIVE_STATE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

@Epic("Filter devices (By device profile and state)")
public class DeviceFilterTest extends AbstractDeviceTest {

    private String activeDeviceName;
    private String deviceWithProfileName;
    private String activeDeviceWithProfileName;

    @BeforeClass
    public void createTestEntities() throws JsonProcessingException {
        DeviceProfile deviceProfile = testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(ENTITY_NAME + random()));
        Device deviceWithProfile = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME + random(), deviceProfile.getId()));
        Device activeDevice = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME + random()));
        Device activeDeviceWithProfile = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME + random(), deviceProfile.getId()));

        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(activeDevice.getId());
        DeviceCredentials deviceCredentials1 = testRestClient.getDeviceCredentialsByDeviceId(activeDeviceWithProfile.getId());
        testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), mapper.readTree(createPayload().toString()));
        testRestClient.postTelemetry(deviceCredentials1.getCredentialsId(), mapper.readTree(createPayload().toString()));

        deviceProfileTitle = deviceProfile.getName();
        deviceWithProfileName = deviceWithProfile.getName();
        activeDeviceName = activeDevice.getName();
        activeDeviceWithProfileName = activeDeviceWithProfile.getName();
    }

    @AfterClass
    public void deleteTestEntities() {
        deleteDevicesByName(deviceWithProfileName, activeDeviceName, activeDeviceWithProfileName);
        deleteDeviceProfileByTitle(deviceProfileTitle);
    }

    @Test(groups = "smoke")
    @Description("Filter by device profile")
    public void filterDevicesByProfile() {
        sideBarMenuView.goToDevicesPage();
        devicePage.filterBtn().click();
        devicePage.filterDeviceByDeviceProfile(deviceProfileTitle);

        devicePage.listOfDevicesProfile().forEach(d -> assertThat(d.getText())
                .as("There are only devices with the selected profile on the page")
                .isEqualTo(deviceProfileTitle));
    }

    @Test(groups = "smoke")
    @Description("Filter by state (Active)")
    public void filterDevicesByActiveState() {
        sideBarMenuView.goToDevicesPage();
        devicePage.filterBtn().click();
        devicePage.filterDeviceByState(DEVICE_ACTIVE_STATE);

        devicePage.listOfDevicesState().forEach(d -> assertThat(d.getText())
                .as("There are only devices with active state on the page")
                .isEqualTo(DEVICE_ACTIVE_STATE));
    }

    @Test(groups = "smoke")
    @Description("Filter by state (Inactive)")
    public void filterDevicesByInactiveState() {
        sideBarMenuView.goToDevicesPage();
        devicePage.filterBtn().click();
        devicePage.filterDeviceByState(DEVICE_INACTIVE_STATE);

        devicePage.listOfDevicesState().forEach(d -> assertThat(d.getText())
                .as("There are only devices with inactive state on the page")
                .isEqualTo(DEVICE_INACTIVE_STATE));
    }

    @Test(groups = "smoke")
    @Description("Filter device by device profile and active state")
    public void filterDevicesByDeviceProfileAndActiveState() {
        sideBarMenuView.goToDevicesPage();
        devicePage.filterBtn().click();
        devicePage.filterDeviceByDeviceProfileAndState(deviceProfileTitle, DEVICE_ACTIVE_STATE);

        devicePage.listOfDevicesProfile().forEach(d -> assertThat(d.getText())
                .as("There are only devices with the selected profile on the page")
                .isEqualTo(deviceProfileTitle));
        devicePage.listOfDevicesState().forEach(d -> assertThat(d.getText())
                .as("There are only devices with active state on the page")
                .isEqualTo(DEVICE_ACTIVE_STATE));
    }

    @Test(groups = "smoke")
    @Description("Filter device by device profile and inactive state")
    public void filterDevicesByDeviceProfileAndInactiveState() {
        sideBarMenuView.goToDevicesPage();
        devicePage.filterBtn().click();
        devicePage.filterDeviceByDeviceProfileAndState(deviceProfileTitle, DEVICE_INACTIVE_STATE);

        devicePage.listOfDevicesProfile().forEach(d -> assertThat(d.getText())
                .as("There are only devices with the selected profile on the page")
                .isEqualTo(deviceProfileTitle));
        devicePage.listOfDevicesState().forEach(d -> assertThat(d.getText())
                .as("There are only devices with inactive state on the page")
                .isEqualTo(DEVICE_INACTIVE_STATE));
    }
}
